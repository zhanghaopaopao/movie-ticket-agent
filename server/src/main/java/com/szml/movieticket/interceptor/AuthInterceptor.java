package com.szml.movieticket.interceptor;

import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * 认证拦截器，从 Header 提取 Token，通过 Redis 校验并滑动续期。
 * 排除路径在 WebMvcConfig 中配置。
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
    private static final String REDIS_AUTH_PREFIX = "auth:";
    private static final int TOKEN_TTL_MINUTES = 30;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            if (isPublicReadRequest(request)) {
                return true;
            }
            writeError(response, ErrorCode.UNAUTHORIZED);
            return false;
        }

        String redisKey = REDIS_AUTH_PREFIX + token;
        String value = stringRedisTemplate.opsForValue().get(redisKey);
        if (value == null) {
            log.debug("Token 无效或已过期");
            writeError(response, ErrorCode.UNAUTHORIZED);
            return false;
        }

        // 解析 userId:role
        String[] parts = value.split(":");
        Long userId = Long.parseLong(parts[0]);
        UserRole role = UserRole.fromCode(Integer.parseInt(parts[1]));
        UserContext.set(userId, role);

        // 滑动续期：每次请求刷新 Redis TTL
        stringRedisTemplate.expire(redisKey, Duration.ofMinutes(TOKEN_TTL_MINUTES));

        log.debug("认证成功, userId: {}, role: {}", userId, role);
        return true;
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

    private boolean isPublicReadRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.matches("/api/user/movies/?")
                || path.matches("/api/user/movies/\\d+/?")
                || path.matches("/api/user/movies/\\d+/reviews/?")
                || path.matches("/api/user/cinemas(?:/nearby)?/?")
                || path.matches("/api/user/showtimes/?");
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(200);
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.error(errorCode.getCode(), errorCode.getMessage())));
    }
}
