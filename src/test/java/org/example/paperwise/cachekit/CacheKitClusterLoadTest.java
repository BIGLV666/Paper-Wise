package org.example.paperwise.cachekit;

import org.example.paperwise.Mapper.UserMapper;
import org.example.paperwise.entry.User;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集群压测：4 个应用实例（独立 L1）+ 共享 Redis（L2 + 广播），验证：
 * 1）持续写入下 pub/sub 广播是否会把失效可靠送达所有实例（最终一致性）；
 * 2）脏读窗口有多严重（读到旧值持续多久）。
 *
 * <p>写线程在实例 0 上单调递增 user:10 版本（username = "v" + seq）；
 * 每实例 8 个读线程高频 selectById，实时计算「观察时刻 − 版本提交时刻」= 脏读时长；
 * 结束后逐实例校验最终版本——广播丢失的实例 L1 会滞留旧值直到 TTL（30s），必被断言抓住。</p>
 */
class CacheKitClusterLoadTest {

    private static final int INSTANCES = 4;
    private static final int READERS_PER_INSTANCE = 8;
    private static final int UPDATES = 300;
    private static final long UPDATE_INTERVAL_MS = 8;
    /** 单次观察脏读超过该值记为一次"显著脏读" */
    private static final long STALE_THRESHOLD_MS = 50;
    /** 整体脏读上限断言：应远小于 L1 TTL（30s），由广播 + 延迟双删兜住 */
    private static final long MAX_STALE_ASSERT_MS = 2000;

    private static final String TEST_KEY_PREFIX = "user:10";

    @BeforeAll
    static void requireRedis() {
        boolean reachable;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 6379), 500);
            reachable = true;
        } catch (Exception e) {
            reachable = false;
        }
        Assumptions.assumeTrue(reachable, "本地无 Redis，跳过集群压测");
    }

    @BeforeEach
    void cleanRedisTestKey() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();
        try {
            StringRedisTemplate template = new StringRedisTemplate(factory);
            template.afterPropertiesSet();
            Set<String> keys = template.keys(TEST_KEY_PREFIX);
            if (keys != null && !keys.isEmpty()) {
                template.delete(keys);
            }
        } finally {
            factory.destroy();
        }
    }

    @Test
    void clusterBroadcastAndStalenessUnderLoad() throws Exception {
        // 4 个独立上下文 = 4 个独立 L1；L2 与广播共享 Redis。
        // ApplicationContextRunner 的 run() 会在 consumer 结束后关闭上下文，
        // 因此用四层嵌套保持 4 个实例同时存活。
        UserMapper[] mappers = new UserMapper[INSTANCES];
        AtomicInteger[] dbCounts = new AtomicInteger[INSTANCES];

        runner().run(ctx0 -> {
            JdbcTemplate jdbc = ctx0.getBean(JdbcTemplate.class);
            CacheKitIntegrationTest.createUserTable(jdbc);
            jdbc.update("INSERT INTO user(user_id, username, password, email, create_time) "
                    + "VALUES (10, 'v0', 'pwd', 'cluster@paperwise.dev', CURRENT_TIMESTAMP)");
            mappers[0] = ctx0.getBean(UserMapper.class);
            dbCounts[0] = ctx0.getBean("userSelectCount", AtomicInteger.class);

            runner().run(ctx1 -> {
                mappers[1] = ctx1.getBean(UserMapper.class);
                dbCounts[1] = ctx1.getBean("userSelectCount", AtomicInteger.class);
                runner().run(ctx2 -> {
                    mappers[2] = ctx2.getBean(UserMapper.class);
                    dbCounts[2] = ctx2.getBean("userSelectCount", AtomicInteger.class);
                    runner().run(ctx3 -> {
                        mappers[3] = ctx3.getBean(UserMapper.class);
                        dbCounts[3] = ctx3.getBean("userSelectCount", AtomicInteger.class);
                        try {
                            runLoadAndVerify(mappers, dbCounts);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                });
            });
        });
    }

    private ApplicationContextRunner runner() {
        return new CacheKitIntegrationTest().runner("cachekit_cluster");
    }

    private void runLoadAndVerify(UserMapper[] mappers, AtomicInteger[] dbCounts) throws Exception {
        long[] maxStaleNs = new long[INSTANCES];
        long[] staleObs = new long[INSTANCES];
        long[] totalObs = new long[INSTANCES];
        AtomicLong[] exceptions = new AtomicLong[INSTANCES];
        for (int i = 0; i < INSTANCES; i++) {
            exceptions[i] = new AtomicLong();
        }

        // 版本提交时刻：写入前记录（版本在写库前不可见，此刻起算只会高估脏读、不会漏报）；
        // latestVersion 供读线程判断"所读版本是否落后于最新已提交版本"——读的就是最新版时不计脏读
        long[] commitAtNs = new long[UPDATES + 1];
        commitAtNs[0] = System.nanoTime();
        AtomicInteger latestVersion = new AtomicInteger(0);

        AtomicBoolean stop = new AtomicBoolean(false);
        CountDownLatch readersReady = new CountDownLatch(INSTANCES * READERS_PER_INSTANCE);
        CountDownLatch readersDone = new CountDownLatch(INSTANCES * READERS_PER_INSTANCE);

        for (int inst = 0; inst < INSTANCES; inst++) {
            final int instance = inst;
            UserMapper mapper = mappers[inst];
            for (int r = 0; r < READERS_PER_INSTANCE; r++) {
                Thread t = new Thread(() -> {
                    readersReady.countDown();
                    try {
                        readersReady.await(15, TimeUnit.SECONDS);
                        long maxStale = 0;
                        long staleCount = 0;
                        long count = 0;
                        while (!stop.get()) {
                            try {
                                User u = mapper.selectById(10L);
                                long tObs = System.nanoTime();
                                // 0.2ms 节流：降低 32 个紧循环线程对单 JVM CPU 的饱和冲击，减少调度噪声
                                java.util.concurrent.locks.LockSupport.parkNanos(200_000);
                                int v = Integer.parseInt(u.getUsername().substring(1));
                                // 只有读到比"最新已提交版本"更旧的值才算脏读；读的是最新版则脏读为 0
                                long stale = v < latestVersion.get() ? tObs - commitAtNs[v] : 0;
                                if (stale > maxStale) {
                                    maxStale = stale;
                                }
                                if (stale > STALE_THRESHOLD_MS * 1_000_000L) {
                                    staleCount++;
                                }
                                count++;
                            } catch (Exception e) {
                                exceptions[instance].incrementAndGet();
                            }
                        }
                        maxStaleNs[instance] = maxStale;
                        staleObs[instance] = staleCount;
                        totalObs[instance] = count;
                    } catch (InterruptedException ignored) {
                    } finally {
                        readersDone.countDown();
                    }
                }, "reader-inst" + inst + "-" + r);
                t.start();
            }
        }
        readersReady.await(15, TimeUnit.SECONDS);
        Thread.sleep(300); // 让各实例先回源填 L1/L2

        // 写线程：单调递增版本，触发 MP 自动失效 + 广播 + 延迟双删
        for (int k = 1; k <= UPDATES; k++) {
            commitAtNs[k] = System.nanoTime();
            latestVersion.set(k);
            mappers[0].updateById(new User(10L, "v" + k, "pwd", "cluster@paperwise.dev", LocalDateTime.now()));
            Thread.sleep(UPDATE_INTERVAL_MS);
        }
        Thread.sleep(2000); // 等最后一次广播 + 延迟双删（150ms）传播完毕

        stop.set(true);
        readersDone.await(15, TimeUnit.SECONDS);

        // 汇总报告
        System.out.println("========== 集群压测结果（" + INSTANCES + " 实例 × " + READERS_PER_INSTANCE
                + " 读线程, " + UPDATES + " 次更新） ==========");
        long globalMaxStaleMs = 0;
        long globalStaleObs = 0;
        long globalTotalObs = 0;
        for (int i = 0; i < INSTANCES; i++) {
            long maxMs = TimeUnit.NANOSECONDS.toMillis(maxStaleNs[i]);
            double staleRatio = totalObs[i] == 0 ? 0 : 100.0 * staleObs[i] / totalObs[i];
            System.out.printf("实例 %d: 观察 %d 次 | 最大脏读 %d ms | 脏读>%dms 占比 %.4f%% | 异常 %d | DB回源 %d 次%n",
                    i, totalObs[i], maxMs, STALE_THRESHOLD_MS, staleRatio, exceptions[i].get(), dbCounts[i].get());
            globalMaxStaleMs = Math.max(globalMaxStaleMs, maxMs);
            globalStaleObs += staleObs[i];
            globalTotalObs += totalObs[i];
        }
        System.out.printf("全局: 最大脏读 %d ms | 脏读占比 %.4f%% (%d/%d)%n",
                globalMaxStaleMs, globalTotalObs == 0 ? 0 : 100.0 * globalStaleObs / globalTotalObs,
                globalStaleObs, globalTotalObs);

        // 断言 1：读线程全程无异常
        for (int i = 0; i < INSTANCES; i++) {
            assertThat(exceptions[i].get()).as("实例 " + i + " 读异常数").isZero();
        }
        // 断言 2：最终一致性——每个实例（含各自 L1）最终读到最终版本；
        // 若广播丢失，该实例 L1 滞留旧值直到 TTL 30s，此处必失败
        for (int i = 0; i < INSTANCES; i++) {
            User u = mappers[i].selectById(10L);
            assertThat(u.getUsername())
                    .as("实例 " + i + " 最终读到的版本").isEqualTo("v" + UPDATES);
        }
        // 断言 3：脏读窗口有上界，远小于 L1 TTL
        assertThat(globalMaxStaleMs)
                .as("最大脏读窗口（阈值 " + MAX_STALE_ASSERT_MS + "ms）").isLessThan(MAX_STALE_ASSERT_MS);
    }
}
