package org.example.paperwise.Advice;

import lombok.extern.slf4j.Slf4j;
import io.github.biglv666.guard.idempotent.IdempotentRejectedException;
import org.example.paperwise.Dto.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice(basePackages = "org.example.paperwise.Controller")
public class GlobalExceptionHandler  {


    @ExceptionHandler(value = Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常{}",e.getMessage(),e);
        return Result.error("系统异常"+e.getMessage());
    }

    @ExceptionHandler(value = NullPointerException.class)
    public Result<?> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常{}",e.getMessage(),e);
        return Result.error("系统繁忙"+e.getMessage());
    }

    /**
     * 幂等拒绝（guard-spring-boot-starter）：预期内的防重复提交拦截，
     * 返回注解声明的提示信息，不打 error 堆栈。
     */
    @ExceptionHandler(value = IdempotentRejectedException.class)
    public Result<?> handleIdempotentRejected(IdempotentRejectedException e) {
        log.warn("幂等拒绝: key={}", e.getKey());
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(value = RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        log.error("业务异常{}",e.getMessage(),e);
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(value = IllegalAccessError.class)
    public Result<?> handleIllegalAccessError(IllegalAccessError e) {
         log.warn("参数异常: {}", e.getMessage());
         return Result.error("系统繁忙"+e.getMessage());
    }
}
