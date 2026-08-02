package com.szml.movieticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置（原在 SecurityConfig 中，移除 Security 后独立）。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {//创建密码编码器
        return new BCryptPasswordEncoder(12);
    }
}
