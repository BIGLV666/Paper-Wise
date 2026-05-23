package org.example.paperwise.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/paperwise/user/login")
                .excludePathPatterns("/paperwise/user/register")
                .excludePathPatterns("/paperwise/user/logout")
                .excludePathPatterns("/doc.html")
                .excludePathPatterns("/v3/api-docs/**")
                .excludePathPatterns("/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**")
                .excludePathPatterns("/paperwise/user/activation");
    }
}
