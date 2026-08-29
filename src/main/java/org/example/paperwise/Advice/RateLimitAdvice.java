package org.example.paperwise.Advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.apigovernancespringbootstarter.filter.FilterContext;
import org.example.apigovernancespringbootstarter.filter.PostFilter;
import org.example.apigovernancespringbootstarter.filter.PreFilter;
import org.example.apigovernancespringbootstarter.ratelimit.RateLimitKeyResolver;
import org.example.paperwise.Interface.RateLimit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RateLimitAdvice implements RateLimitKeyResolver {

    @Override
    public String resolve(FilterContext context) {
        HttpServletRequest request=((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return context.getApiKey()+request.getHeader("userid");
    }
}
