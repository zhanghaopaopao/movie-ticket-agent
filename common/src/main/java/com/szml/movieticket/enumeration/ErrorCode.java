package com.szml.movieticket.enumeration;

import lombok.Getter;

/**
 * 统一业务状态码枚举，消息自包含，业务代码只需传 ErrorCode 即可。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),

    /** 参数校验失败 */
    PARAM_ERROR(400, "参数校验失败"),

    /** 未登录 / Token 过期 */
    UNAUTHORIZED(401, "未登录或Token过期"),

    /** 无权限 */
    FORBIDDEN(403, "无权限"),

    /** 服务器内部错误 */
    SYSTEM_ERROR(500, "服务器内部错误"),

    // ---- 认证（1000-1999）----

    AUTH_ACCOUNT_NOT_FOUND(1000, "手机号或密码不正确"),
    AUTH_WRONG_PASSWORD(1001, "手机号或密码不正确"),
    AUTH_ACCOUNT_DISABLED(1002, "账号已被禁用，请联系管理员"),
    AUTH_ACCOUNT_LOCKED(1003, "账号已被锁定，请稍后重试"),

    // ---- 影片（2000-2999）----

    MOVIE_NOT_FOUND(2000, "影片不存在"),
    MOVIE_NAME_DUPLICATE(2001, "影片名称已存在"),
    MOVIE_HAS_ACTIVE_SHOWTIMES(2002, "该影片存在在售场次，不可下架"),

    // ---- 文件（6000-6999）----

    FILE_UPLOAD_ERROR(6000, "文件上传失败"),
    FILE_FORMAT_INVALID(6001, "仅支持 jpg/png/webp 格式的图片文件"),
    FILE_SIZE_EXCEED(6002, "文件大小不能超过 5MB");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
