package org.example.paperwise.Advice;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Aspect
public class AopAdvice {
    @Pointcut("execution(* org.example.paperwise.Service.*.*(..))")
    public void pointCutService() {
    }

    @Pointcut("execution(* org.example.paperwise.Controller.*.*(..))")
    public void pointCutController() {
    }

    private static final Logger ControllerLogger = LoggerFactory.getLogger("ControllerLogger");
    private static final Logger ServiceLogger = LoggerFactory.getLogger("ServiceLogger");

    @Around("pointCutService()")
    public Object aroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object obj = joinPoint.proceed();
        long end = System.currentTimeMillis();
        if (end - start > 200) {
            ServiceLogger.info("methodName={}Time={}", joinPoint.getSignature().getName(), end - start);
            if (obj != null) {
                ServiceLogger.info("Object{}", obj);
            }
        }

        return obj;
    }

    @Around("pointCutController()")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object obj;

        obj = joinPoint.proceed();
        long end = System.currentTimeMillis();
        if (end - start > 200) {
            ControllerLogger.info("methodName={}Time={}", joinPoint.getSignature().getName(), end - start);
            if (obj != null) {
                ControllerLogger.info("Object{}", obj);
            }


        }
        return obj;
    }
}