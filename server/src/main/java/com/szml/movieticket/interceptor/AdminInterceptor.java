package com.szml.movieticket.interceptor;

import com.szml.movieticket.result.Result;
import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员权限拦截器，仅拦截 /api/admin/**。
 * 必须在 AuthInterceptor 之后执行。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        UserRole role = UserContext.getRole();
        if (role != UserRole.ADMIN) {
            log.warn("非管理员访问后台, userId: {}, role: {}", UserContext.getUserId(), role);
            writeError(response, ErrorCode.FORBIDDEN);
            return false;
        }
        return true;
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(200);
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.error(errorCode.getCode(), errorCode.getMessage())));
    }
}
