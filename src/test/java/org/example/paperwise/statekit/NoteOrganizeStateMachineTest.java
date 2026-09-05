package org.example.paperwise.statekit;

import io.github.biglv666.statekit.FireArg;
import io.github.biglv666.statekit.StateMachine;
import io.github.biglv666.statekit.config.StateKitAutoConfiguration;
import io.github.biglv666.statekit.exception.IllegalTransitionException;
import org.example.paperwise.enums.NoteOrganizeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * state-kit 与 PaperWise 真实表结构（note_ai_organization）的集成测试。
 *
 * <p>用 H2 内存库按生产 DDL 建表，验证 NoteService.processOrganization 的
 * 状态流转唯一写入口：PROCESSING --SUCCEED--> SUCCESS / --FAIL--> FAILED。</p>
 */
class NoteOrganizeStateMachineTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    JdbcTemplateAutoConfiguration.class,
                    StateKitAutoConfiguration.class))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:paperwise_test;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    // 与生产 application.yml 的 state-kit.machines.noteOrganize 完全一致
                    "state-kit.machines.noteOrganize.state-type=org.example.paperwise.enums.NoteOrganizeStatus",
                    "state-kit.machines.noteOrganize.table=note_ai_organization",
                    "state-kit.machines.noteOrganize.status-column=status",
                    "state-kit.machines.noteOrganize.id-column=organization_id",
                    "state-kit.machines.noteOrganize.id-type=java.lang.Long",
                    "state-kit.machines.noteOrganize.conflict-strategy=throw",
                    "state-kit.machines.noteOrganize.transitions[0].from=PROCESSING",
                    "state-kit.machines.noteOrganize.transitions[0].event=SUCCEED",
                    "state-kit.machines.noteOrganize.transitions[0].to=SUCCESS",
                    "state-kit.machines.noteOrganize.transitions[1].from=PROCESSING",
                    "state-kit.machines.noteOrganize.transitions[1].event=FAIL",
                    "state-kit.machines.noteOrganize.transitions[1].to=FAILED");

    @BeforeEach
    void createTable() {
        runner.run(context -> context.getBean(JdbcTemplate.class).execute("""
                CREATE TABLE IF NOT EXISTS note_ai_organization (
                    organization_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    note_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    source_content CLOB NOT NULL,
                    organized_content CLOB,
                    error_message CLOB,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """));
    }

    private long seed(JdbcTemplate jdbc, String status) {
        jdbc.update("INSERT INTO note_ai_organization (note_id, user_id, status, source_content) "
                + "VALUES (1, 1, ?, '笔记原文')", status);
        Long id = jdbc.queryForObject("SELECT MAX(organization_id) FROM note_ai_organization", Long.class);
        return id == null ? 0 : id;
    }

    @SuppressWarnings("unchecked")
    private StateMachine<NoteOrganizeStatus, Long> machine(org.springframework.context.ApplicationContext context) {
        return (StateMachine<NoteOrganizeStatus, Long>) context.getBean("noteOrganize");
    }

    @Test
    void 上下文启动且状态机按生产配置注册() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            StateMachine<NoteOrganizeStatus, Long> m = machine(context);
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            long id = seed(jdbc, "PROCESSING");
            assertThat(m.currentState(id)).contains(NoteOrganizeStatus.PROCESSING);
            assertThat(m.isFinal(id)).isFalse();
            assertThat(m.nextStates(id))
                    .containsExactlyInAnyOrder(NoteOrganizeStatus.SUCCESS, NoteOrganizeStatus.FAILED);
        });
    }

    @Test
    void SUCCEED流转写入状态与整理内容() {
        runner.run(context -> {
            StateMachine<NoteOrganizeStatus, Long> m = machine(context);
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            long id = seed(jdbc, "PROCESSING");

            m.fire(id, "SUCCEED",
                    FireArg.set("organized_content", "# 整理后的笔记"),
                    FireArg.set("error_message", null));

            assertThat(m.currentState(id)).contains(NoteOrganizeStatus.SUCCESS);
            assertThat(jdbc.queryForObject(
                    "SELECT organized_content FROM note_ai_organization WHERE organization_id = ?",
                    String.class, id)).isEqualTo("# 整理后的笔记");
        });
    }

    @Test
    void FAIL流转写入错误信息() {
        runner.run(context -> {
            StateMachine<NoteOrganizeStatus, Long> m = machine(context);
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            long id = seed(jdbc, "PROCESSING");

            m.fire(id, "FAIL", FireArg.set("error_message", "AI 服务超时"));

            assertThat(m.currentState(id)).contains(NoteOrganizeStatus.FAILED);
            assertThat(jdbc.queryForObject(
                    "SELECT error_message FROM note_ai_organization WHERE organization_id = ?",
                    String.class, id)).isEqualTo("AI 服务超时");
        });
    }

    @Test
    void 终态不可再流转_对应重复回调被拒() {
        runner.run(context -> {
            StateMachine<NoteOrganizeStatus, Long> m = machine(context);
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            long id = seed(jdbc, "PROCESSING");
            m.fire(id, "SUCCEED");

            // SUCCESS 后任何事件（如迟到的 FAIL 回调）都不可能再改状态
            assertThat(m.isFinal(id)).isTrue();
            assertThatThrownBy(() -> m.fire(id, "FAIL"))
                    .isInstanceOf(IllegalTransitionException.class);
            assertThat(m.currentState(id)).contains(NoteOrganizeStatus.SUCCESS);
        });
    }

    @Test
    void 实体不存在时流转被拒() {
        runner.run(context -> {
            StateMachine<NoteOrganizeStatus, Long> m = machine(context);
            assertThatThrownBy(() -> m.fire(999L, "SUCCEED"))
                    .isInstanceOf(IllegalTransitionException.class)
                    .hasMessageContaining("不存在");
        });
    }

    @Test
    void 并发重复回调恰好一成一败() throws Exception {
        runner.run(context -> {
            StateMachine<NoteOrganizeStatus, Long> m = machine(context);
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            long id = seed(jdbc, "PROCESSING");

            // 模拟异步任务被调度两次（重复回调）：CAS 保证 status 只被推进一次
            int threads = 6;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            java.util.List<Future<Boolean>> results = new java.util.ArrayList<>();
            for (int i = 0; i < threads; i++) {
                final int n = i;
                results.add(pool.submit(() -> {
                    start.await();
                    try {
                        m.fire(id, "SUCCEED",
                                FireArg.set("organized_content", "v" + n));
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }));
            }
            start.countDown();
            int success = 0;
            for (Future<Boolean> f : results) {
                if (f.get(30, TimeUnit.SECONDS)) {
                    success++;
                }
            }
            pool.shutdown();

            assertThat(success).isEqualTo(1);
            assertThat(m.currentState(id)).contains(NoteOrganizeStatus.SUCCESS);
        });
    }
}
