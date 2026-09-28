package org.example.paperwise.Advice;

import io.github.biglv666.apigovernance.filter.FilterContext;
import io.github.biglv666.apigovernance.ratelimit.RateLimitKeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 用户维度限流键解析器：限流键 = 接口标识 + 请求头 userid，
 * 同一接口按用户独立配额（对接 api-governance starter 的 RateLimitKeyResolver SPI）。
 */
@Component
@Slf4j
public class UserRateLimitKeyResolver implements RateLimitKeyResolver {

    @Override
    public String resolve(FilterContext context) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            // 非 Web 线程（测试直调等）：退化为接口维度限流
            return context.getApiKey();
        }
        String userid = request.getHeader("userid");
        return userid == null ? context.getApiKey() : context.getApiKey() + "#user:" + userid;
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        return attrs.getRequest();
    }
}
