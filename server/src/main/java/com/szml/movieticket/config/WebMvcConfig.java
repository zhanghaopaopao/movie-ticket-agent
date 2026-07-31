package com.szml.movieticket.config;

import com.szml.movieticket.interceptor.AdminInterceptor;
import com.szml.movieticket.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器注册配置。
 * AuthInterceptor 先执行（解析 token），AdminInterceptor 后执行（校验角色）。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 认证拦截器：所有请求都过一遍
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .order(1);

        // 管理员权限拦截器：仅 /api/admin/**
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**")
                .order(2);
    }
}
