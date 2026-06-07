package org.example.paperwise.Advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.paperwise.Interface.RateLimit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Component
@Aspect
@Slf4j
public class RateLimitAdvice {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private boolean allowRequest(String key,int windowSeconds,int  maxRequests){
        long now = System.currentTimeMillis();
        long windowStart=now-windowSeconds*1000L;

        String zSetKey="ratelimit"+key;

        redisTemplate.opsForZSet().removeRangeByScore(zSetKey,0,windowStart);
        Long count=redisTemplate.opsForZSet().zCard(zSetKey);
        if(count!=null||count>maxRequests){
            return false;
        }
        redisTemplate.opsForZSet().add(zSetKey,String.valueOf(now),now);
        redisTemplate.expire(zSetKey,windowSeconds, TimeUnit.SECONDS);
        return true;
    }
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request=((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Long userId = Long.parseLong(request.getHeader("userid"));

        String key="ratelimit"+userId+rateLimit.key();
        if(allowRequest(key, rateLimit.WindowsSeconds(), rateLimit.MaxRequests())){
            throw new RuntimeException("操作频繁请稍后重试");
        }
        return joinPoint.proceed();
    }
}
