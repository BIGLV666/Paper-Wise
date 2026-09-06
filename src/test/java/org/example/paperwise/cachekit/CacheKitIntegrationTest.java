package org.example.paperwise.cachekit;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.github.biglv666.cachekit.config.CacheKitAutoConfiguration;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.example.paperwise.Mapper.UserMapper;
import org.example.paperwise.entry.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * cache-kit 与 PaperWise 真实链路（MP 3.5.5 + UserMapper + 真实 Redis 容器）的集成测试。
 *
 * <p>DB 层用 H2（NON_KEYWORDS=USER 兼容 user 表名），Redis 用 localhost:6379 的
 * cache-kit-redis 容器；用 MyBatis Interceptor 统计 selectById 实际打到 DB 的次数。</p>
 */
class CacheKitIntegrationTest {

    /** 统计按 ID 查询打到 DB 的次数（每个 ApplicationContext 一个实例） */
    @Intercepts(@Signature(type = Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
    static class DbCountingInterceptor implements Interceptor {

        private final AtomicInteger counter;

        DbCountingInterceptor(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            String id = ms.getId();
            if (id.endsWith("selectById") || id.endsWith("getUserById")) {
                counter.incrementAndGet();
            }
            return invocation.proceed();
        }
    }

    @Configuration
    @MapperScan("org.example.paperwise.Mapper")
    static class MappersConfig {
    }

    @Configuration
    static class CountingConfig {

        @Bean
        AtomicInteger userSelectCount() {
            return new AtomicInteger();
        }

        @Bean
        DbCountingInterceptor dbCountingInterceptor(AtomicInteger userSelectCount) {
            return new DbCountingInterceptor(userSelectCount);
        }
    }

    ApplicationContextRunner runner(String h2Db) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        JdbcTemplateAutoConfiguration.class,
                        MybatisPlusAutoConfiguration.class,
                        RedisAutoConfiguration.class,
                        AopAutoConfiguration.class,
                        CacheKitAutoConfiguration.class))
                .withUserConfiguration(MappersConfig.class, CountingConfig.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:" + h2Db + ";DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.datasource.username=sa",
                        "mybatis-plus.mapper-locations=classpath*:/Mapper/*.xml",
                        "spring.data.redis.host=localhost",
                        "spring.data.redis.port=6379",
                        "cache-kit.l1.ttl=30s",
                        "cache-kit.l2.ttl=60s",
                        "cache-kit.l2.jitter=1ms",
                        "cache-kit.l2.null-ttl=1s",
                        "cache-kit.l2.double-delete-delay=150ms");
    }

    static void createUserTable(JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS user ("
                + "user_id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "username VARCHAR(64), password VARCHAR(128), email VARCHAR(128), create_time TIMESTAMP)");
    }

    /**
     * L2 缓存在 Redis 容器中跨 JVM 存活（TTL 内有效），跑测试前清掉本测试使用的键，
     * 避免上一轮运行留下的缓存污染断言。
     */
    @BeforeEach
    void cleanRedisL2Keys() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();
        try {
            StringRedisTemplate template = new StringRedisTemplate(factory);
            template.afterPropertiesSet();
            Set<String> keys = template.keys("user:*");
            if (keys != null && !keys.isEmpty()) {
                template.delete(keys);
            }
        } finally {
            factory.destroy();
        }
    }

    /**
     * 高并发读：64 线程 × 500 次 selectById = 32000 次调用，DB 只允许被打 1 次
     *（三级缓存 + single-flight），且所有线程读到一致的数据。
     */
    @Test
    void highConcurrencyReadsShouldHitDbOnlyOnce() throws Exception {
        runner("cachekit_conc").run(ctx -> {
            JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);
            createUserTable(jdbc);
            jdbc.update("INSERT INTO user(user_id, username, password, email, create_time) "
                    + "VALUES (1, 'lv', 'pwd', 'lv@paperwise.dev', CURRENT_TIMESTAMP)");

            UserMapper mapper = ctx.getBean(UserMapper.class);
            AtomicInteger dbCount = ctx.getBean("userSelectCount", AtomicInteger.class);

            int threads = 64;
            int readsPerThread = 500;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch done = new CountDownLatch(threads);
            AtomicInteger failures = new AtomicInteger();

            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        ready.await(10, TimeUnit.SECONDS);
                        for (int j = 0; j < readsPerThread; j++) {
                            User u = mapper.selectById(1L);
                            if (u == null || !"lv".equals(u.getUsername())) {
                                failures.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
            pool.shutdownNow();

            assertThat(failures.get()).as("并发读取中读到脏数据/异常的次数").isZero();
            assertThat(dbCount.get()).as("32000 次读取打到 DB 的次数").isEqualTo(1);
        });
    }

    /**
     * 双实例一致性：实例 A 读过 user:2 后，实例 B 更新该用户。
     * B 的删除 + 广播必须清掉 A 的 L1——A 再读必须回源拿到新值，
     * 而不是继续吃本地 L1 的旧值。
     */
    @Test
    void updateInInstanceBShouldInvalidateInstanceALocalCache() throws Exception {
        runner("cachekit_multi").run(ctxA -> {
            JdbcTemplate jdbc = ctxA.getBean(JdbcTemplate.class);
            createUserTable(jdbc);
            jdbc.update("INSERT INTO user(user_id, username, password, email, create_time) "
                    + "VALUES (2, 'lv-old', 'pwd', 'lv@paperwise.dev', CURRENT_TIMESTAMP)");

            UserMapper mapperA = ctxA.getBean(UserMapper.class);
            AtomicInteger dbCountA = ctxA.getBean("userSelectCount", AtomicInteger.class);

            // A 首读：回源并写入 A 的 L1 + 共享 L2
            User a1 = mapperA.selectById(2L);
            assertThat(a1.getUsername()).isEqualTo("lv-old");
            assertThat(dbCountA.get()).isEqualTo(1);

            // 等订阅链路就绪
            Thread.sleep(1000);

            runner("cachekit_multi").run(ctxB -> {
                UserMapper mapperB = ctxB.getBean(UserMapper.class);
                AtomicInteger dbCountB = ctxB.getBean("userSelectCount", AtomicInteger.class);

                // B 首读：L1 空、L2 命中，不允许回源
                User b1 = mapperB.selectById(2L);
                assertThat(b1.getUsername()).isEqualTo("lv-old");
                assertThat(dbCountB.get()).as("B 应命中共享 L2，不查 DB").isZero();

                // B 更新：触发自动失效 + 广播 + 延迟双删
                User updated = new User(2L, "lv-new", "pwd", "lv@paperwise.dev", LocalDateTime.now());
                assertThat(mapperB.updateById(updated)).isEqualTo(1);

                // 等广播送达 + 延迟双删（150ms）执行完
                Thread.sleep(600);

                // A 再读：A 的 L1 必须已被广播清掉 → 回源拿到新值
                User a2 = mapperA.selectById(2L);
                assertThat(a2.getUsername()).as("A 在 B 更新后必须读到新值").isEqualTo("lv-new");
                assertThat(dbCountA.get()).as("A 的 L1 被广播清掉后必须回源").isEqualTo(2);

                // A 再读一次：新值已回填 A 的 L1，不再打 DB
                assertThat(mapperA.selectById(2L).getUsername()).isEqualTo("lv-new");
                assertThat(dbCountA.get()).isEqualTo(2);
            });
        });
    }

    /**
     * 改装点验证：UserMapper.getUserById（自定义 XML 方法，已加 @CachedQuery）也走三级缓存。
     */
    @Test
    void annotatedCustomQueryShouldUseCache() {
        runner("cachekit_annotated").run(ctx -> {
            JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);
            createUserTable(jdbc);
            jdbc.update("INSERT INTO user(user_id, username, password, email, create_time) "
                    + "VALUES (3, 'ann', 'pwd', 'ann@paperwise.dev', CURRENT_TIMESTAMP)");

            UserMapper mapper = ctx.getBean(UserMapper.class);
            AtomicInteger dbCount = ctx.getBean("userSelectCount", AtomicInteger.class);

            User first = mapper.getUserById(3L);
            User second = mapper.getUserById(3L);

            assertThat(first.getUsername()).isEqualTo("ann");
            assertThat(dbCount.get()).as("@CachedQuery 自定义方法命中缓存").isEqualTo(1);
            assertThat(second.getUsername()).isEqualTo("ann");
        });
    }
}
