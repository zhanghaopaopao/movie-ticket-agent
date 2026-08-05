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
        // 认证拦截器：除登录/注册相关路径外的所有接口都需登录
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/admin/auth/**",
                        "/api/payment/alipay/notify", "/api/payment/alipay/return")
                .order(1);

        // 管理员权限拦截器：仅 /api/admin/**（B端登录接口除外）
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/**")
                .order(2);
    }
}
