package org.example.paperwise.cachekit;

import org.example.paperwise.Mapper.UserMapper;
import org.example.paperwise.entry.User;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 极限压测：10 实例（独立 L1）+ 共享 Redis，10 个热键，阶梯加压找拐点：
 * A 阶段 限速 ~1000 写/s；B 阶段 单写者全速；C 阶段 双实例并发写者。
 * 每阶段后做"全实例 × 全键"最终一致性校验（广播丢失必被抓住），并输出脏读与回源放大。
 */
class CacheKitLimitTest {

    private static final int INSTANCES = 10;
    private static final int READERS_PER_INSTANCE = 4;
    private static final int KEYS = 10;              // user_id 10..19
    private static final long STALE_THRESHOLD_MS = 50;
    private static final long STALE_ASSERT_MS = 10000; // 极限探索只设宽松上界，重点看数据

    @BeforeAll
    static void requireRedis() {
        boolean reachable;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 6379), 500);
            reachable = true;
        } catch (Exception e) {
            reachable = false;
        }
        Assumptions.assumeTrue(reachable, "本地无 Redis，跳过极限压测");
    }

    @BeforeEach
    void cleanRedisTestKeys() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();
        try {
            StringRedisTemplate template = new StringRedisTemplate(factory);
            template.afterPropertiesSet();
            Set<String> keys = template.keys("user:1?");
            if (keys != null && !keys.isEmpty()) {
                template.delete(keys);
            }
        } finally {
            factory.destroy();
        }
    }

    /** 阶段内累积的读侧度量（切阶段换新实例，读线程无锁切引用） */
    static class Metrics {
        final AtomicLong total = new AtomicLong();
        final AtomicLong staleCount = new AtomicLong();
        final AtomicLong maxStaleNs = new AtomicLong();
        final AtomicLong exceptions = new AtomicLong();
    }

    @Test
    void findClusterLimits() throws Exception {
        CacheKitIntegrationTest support = new CacheKitIntegrationTest();
        UserMapper[] mappers = new UserMapper[INSTANCES];
        AtomicInteger[] dbCounts = new AtomicInteger[INSTANCES];
        List<ApplicationContextRunner> runners = new ArrayList<>();
        for (int i = 0; i < INSTANCES; i++) {
            runners.add(support.runner("cachekit_limit"));
        }

        // 用递归展开 10 层嵌套 run()：最内层 body 执行时 10 个上下文全部存活
        List<ConfigurableApplicationContext> contexts = new ArrayList<>();
        launchAndRun(0, runners, mappers, dbCounts, contexts, () -> {
            try {
                runPhases(contexts.get(0), mappers, dbCounts);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void launchAndRun(int i, List<ApplicationContextRunner> runners,
                              UserMapper[] mappers, AtomicInteger[] dbCounts,
                              List<ConfigurableApplicationContext> contexts, Runnable body) {
        if (i == INSTANCES) {
            body.run();
            return;
        }
        runners.get(i).run(ctx -> {
            mappers[i] = ctx.getBean(UserMapper.class);
            dbCounts[i] = ctx.getBean("userSelectCount", AtomicInteger.class);
            contexts.add((ConfigurableApplicationContext) ctx);
            launchAndRun(i + 1, runners, mappers, dbCounts, contexts, body);
        });
    }

    private void runPhases(ConfigurableApplicationContext ctx0, UserMapper[] mappers,
                           AtomicInteger[] dbCounts) throws Exception {
        JdbcTemplate jdbc = ctx0.getBean(JdbcTemplate.class);
        CacheKitIntegrationTest.createUserTable(jdbc);
        for (int k = 0; k < KEYS; k++) {
            jdbc.update("INSERT INTO user(user_id, username, password, email, create_time) "
                    + "VALUES (" + (10L + k) + ", 'v0', 'pwd', 'limit@paperwise.dev', CURRENT_TIMESTAMP)");
        }

        int[] perKeyRound = new int[KEYS];
        AtomicInteger[] latestVer = new AtomicInteger[KEYS];
        long[][] commitAt = new long[KEYS][800];
        for (int k = 0; k < KEYS; k++) {
            latestVer[k] = new AtomicInteger(0);
            commitAt[k][0] = System.nanoTime();
        }

        AtomicReference<Metrics> metricsRef = new AtomicReference<>(new Metrics());
        AtomicBoolean stop = new AtomicBoolean(false);
        CountDownLatch ready = new CountDownLatch(INSTANCES * READERS_PER_INSTANCE);
        CountDownLatch done = new CountDownLatch(INSTANCES * READERS_PER_INSTANCE);

        for (int inst = 0; inst < INSTANCES; inst++) {
            for (int r = 0; r < READERS_PER_INSTANCE; r++) {
                final UserMapper mapper = mappers[inst];
                final int startOffset = inst * READERS_PER_INSTANCE + r;
                Thread t = new Thread(() -> {
                    ready.countDown();
                    try {
                        ready.await(30, TimeUnit.SECONDS);
                        int idx = startOffset;
                        while (!stop.get()) {
                            try {
                                int key = idx++ % KEYS;
                                User u = mapper.selectById(10L + key);
                                long tObs = System.nanoTime();
                                LockSupport.parkNanos(200_000);
                                int v = Integer.parseInt(u.getUsername().substring(1));
                                long stale = v < latestVer[key].get() ? tObs - commitAt[key][v] : 0;
                                Metrics m = metricsRef.get();
                                m.total.incrementAndGet();
                                m.maxStaleNs.accumulateAndGet(stale, Math::max);
                                if (stale > STALE_THRESHOLD_MS * 1_000_000L) {
                                    m.staleCount.incrementAndGet();
                                }
                            } catch (Exception e) {
                                metricsRef.get().exceptions.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                }, "limit-reader-" + inst + "-" + r);
                t.start();
            }
        }
        ready.await(30, TimeUnit.SECONDS);
        Thread.sleep(300);

        // ===== A 阶段：限速 ~1ms/写（~1000 写/s），600 写 =====
        long[] latA = runPaced(mappers, perKeyRound, latestVer, commitAt, 60, 1_000_000L, 0, KEYS);
        Metrics afterA = snapshot(metricsRef);
        verifyConsistency(mappers, latestVer, "A(限速)");
        printPhase("A 限速~1000写/s", 600, latA, afterA, dbCounts);

        // ===== B 阶段：单写者全速，每键 +200 轮（2000 写） =====
        long t0 = System.nanoTime();
        long[] latB = runPaced(mappers, perKeyRound, latestVer, commitAt, 200, 0L, 0, KEYS);
        long durB = (System.nanoTime() - t0) / 1_000_000L;
        Metrics afterB = snapshot(metricsRef);
        verifyConsistency(mappers, latestVer, "B(单写全速)");
        printPhase("B 单写者全速(实际 " + (2000 * 1000L / Math.max(durB, 1)) + " 写/s)", 2000, latB, afterB, dbCounts);

        // ===== C 阶段：双实例并发写者（各管一半键），各 1000 写全速 =====
        long tC0 = System.nanoTime();
        Thread w0 = new Thread(() -> runPaced(mappers, perKeyRound, latestVer, commitAt, 200, 0L, 0, KEYS / 2));
        Thread w1 = new Thread(() -> runPaced(mappers, perKeyRound, latestVer, commitAt, 200, 0L, KEYS / 2, KEYS / 2));
        w0.start();
        w1.start();
        w0.join(60_000);
        w1.join(60_000);
        long durC = (System.nanoTime() - tC0) / 1_000_000L;
        Metrics afterC = snapshot(metricsRef);
        verifyConsistency(mappers, latestVer, "C(双写者)");
        printPhase("C 双写者并发(实际 " + (2000 * 1000L / Math.max(durC, 1)) + " 写/s)", 2000, null, afterC, dbCounts);

        stop.set(true);
        done.await(30, TimeUnit.SECONDS);

        Metrics finalM = metricsRef.get();
        System.out.printf("全局: 总观察 %d | 累计脏读>50ms %d | 最大脏读 %d ms | 读异常 %d%n",
                finalM.total.get(), finalM.staleCount.get(),
                TimeUnit.NANOSECONDS.toMillis(finalM.maxStaleNs.get()), finalM.exceptions.get());

        assertThat(finalM.exceptions.get()).as("读线程异常总数").isZero();
        assertThat(TimeUnit.NANOSECONDS.toMillis(finalM.maxStaleNs.get()))
                .as("最大脏读窗口（宽松上界）").isLessThan(STALE_ASSERT_MS);
    }

    /** 写 [keyFrom, keyFrom+keyCount) 每键 round 轮；paceNanos>0 时按间隔自旋节流；返回写延迟统计 {avgMs, maxMs} */
    private long[] runPaced(UserMapper[] mappers, int[] perKeyRound, AtomicInteger[] latestVer,
                            long[][] commitAt, int rounds, long paceNanos, int keyFrom, int keyCount) {
        long sum = 0, max = 0;
        int count = 0;
        int inst = 0;
        for (int r = 0; r < rounds; r++) {
            for (int k = keyFrom; k < keyFrom + keyCount; k++) {
                long next = System.nanoTime() + paceNanos;
                long t0 = System.nanoTime();
                commitAt[k][perKeyRound[k] + 1] = t0;
                latestVer[k].set(perKeyRound[k] + 1);
                mappers[inst++ % INSTANCES].updateById(
                        new User(10L + k, "v" + (perKeyRound[k] + 1), "pwd", "limit@paperwise.dev", LocalDateTime.now()));
                long lat = System.nanoTime() - t0;
                sum += lat;
                max = Math.max(max, lat);
                count++;
                perKeyRound[k]++;
                if (paceNanos > 0) {
                    while (System.nanoTime() < next) {
                        // 自旋节流（Windows sleep 粒度太粗）
                    }
                }
            }
        }
        return new long[]{count == 0 ? 0 : sum / count / 1_000_000L, max / 1_000_000L};
    }

    private Metrics snapshot(AtomicReference<Metrics> ref) {
        return ref.getAndSet(new Metrics());
    }

    /** 全实例 × 全键最终一致性校验 */
    private void verifyConsistency(UserMapper[] mappers, AtomicInteger[] latestVer, String phase) {
        for (int k = 0; k < KEYS; k++) {
            String expected = "v" + latestVer[k].get();
            for (int i = 0; i < INSTANCES; i++) {
                User u = mappers[i].selectById(10 + k);
                assertThat(u.getUsername())
                        .as("阶段 %s: 实例 %d 键 %d 最终版本（广播丢失的实例 L1 滞留旧值）", phase, i, 10 + k)
                        .isEqualTo(expected);
            }
        }
    }

    private void printPhase(String phase, int updates, long[] latency, Metrics m,
                            AtomicInteger[] dbCounts) {
        System.out.printf("--- 阶段 %s: 写 %d 次 | 写延迟 avg %d ms / max %d ms | 观察 %d | 脏读>50ms %d (%.3f%%) | 阶段内最大脏读 %d ms%n",
                phase, updates,
                latency == null ? -1 : latency[0], latency == null ? -1 : latency[1],
                m.total.get(), m.staleCount.get(),
                m.total.get() == 0 ? 0 : 100.0 * m.staleCount.get() / m.total.get(),
                TimeUnit.NANOSECONDS.toMillis(m.maxStaleNs.get()));
        int after = 0;
        for (AtomicInteger dbCount : dbCounts) {
            after += dbCount.get();
        }
        System.out.printf("    实例间 DB 回源累计: %d 次%n", after);
    }
}
