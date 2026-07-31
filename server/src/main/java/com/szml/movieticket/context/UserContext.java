package com.szml.movieticket.context;

import com.szml.movieticket.enums.UserRole;

/**
 * 用户上下文，通过 ThreadLocal 存储当前请求的 userId 和 role。
 * 请求结束后由 AuthInterceptor 的 afterCompletion 清理。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<UserRole> ROLE = new ThreadLocal<>();

    public static void set(Long userId, UserRole role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static UserRole getRole() {
        return ROLE.get();
    }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
