package com.szml.movieticket.interceptor;

import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.szml.movieticket.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器，从 Header 提取 Token 解析后写入 UserContext。
 * /api/auth/** 路径：有 token 则解析，无则放行。
 * /api/admin/**, /api/v1/** 路径：无 token 返回 401。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_PATH = "/api/auth";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String token = extractToken(request);

        // /api/auth/** 不强制要求 token
        if (!StringUtils.hasText(token)) {
            if (request.getRequestURI().startsWith(AUTH_PATH)) {
                return true;
            }
            writeError(response, ErrorCode.UNAUTHORIZED);
            return false;
        }

        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = jwtUtil.getUserId(claims);
            UserRole role = jwtUtil.getRole(claims);
            UserContext.set(userId, role);
            log.debug("JWT 认证成功, userId: {}, role: {}", userId, role);
            return true;
        } catch (JwtException e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            if (request.getRequestURI().startsWith(AUTH_PATH)) {
                return true;
            }
            writeError(response, ErrorCode.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(200);
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.error(errorCode.getCode(), errorCode.getMessage())));
    }
}
