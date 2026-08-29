package org.example.paperwise.Advice;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.apigovernancespringbootstarter.filter.FilterContext;
import org.example.apigovernancespringbootstarter.filter.PostFilter;
import org.example.paperwise.Interface.LookCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Component
@Slf4j
public class LookCountAdvice implements PostFilter {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    private static final String FAVORITES_LIKE_COUNT_KEY = "favorites_look_count";

    @Override
    public void doFilter(FilterContext context) {
        Method method=context.getMethod();
        LookCount lookCount=method.getAnnotation(LookCount.class);
        if(lookCount==null){
            return;
        }
        Object[] args = context.getJoinPoint().getArgs();
        MethodSignature signature = (MethodSignature) context.getJoinPoint().getSignature();
        String[] paramNames = signature.getParameterNames();

        Long id = null;

        // 遍历多个可能的参数名
        String[] targetNames = lookCount.value();
        for (String targetName : targetNames) {
            for (int i = 0; i < paramNames.length; i++) {
                if (targetName.equals(paramNames[i]) && args[i] instanceof Long) {
                    id = (Long) args[i];
                    break;
                }
            }
            if (id != null) {
                break;
            }
        }
        redisTemplate.opsForValue().increment(FAVORITES_LIKE_COUNT_KEY+id);
    }
}
