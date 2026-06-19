package org.example.paperwise;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PaperWise 应用程序主启动类
 * <p>
 * 该类是整个 PaperWise 项目的入口点，负责启动 Spring Boot 应用。
 * 通过各种注解启用了异步处理、定时任务、AOP 代理和 MyBatis Mapper 扫描等功能。
 * </p>
 *
 * @SpringBootApplication   标记为 Spring Boot 应用，启用自动配置和组件扫描
 * @EnableAsync             启用异步方法执行支持
 * @EnableScheduling        启用定时任务调度
 * @EnableAspectJAutoProxy  启用 AOP 自动代理（exposeProxy=true 允许通过 AopContext 访问当前代理对象）
 * @MapperScan              扫描指定包下的 MyBatis Mapper 接口
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("org.example.paperwise.Mapper")
public class PaperWiseApplication {

    /**
     * 应用程序入口方法
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PaperWiseApplication.class, args);
    }

}
