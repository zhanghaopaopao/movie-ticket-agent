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

    SUCCESS(1, "成功"),

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

    // ---- 影院（3000-3999）----

    CINEMA_NOT_FOUND(3000, "影院不存在"),
    CINEMA_NAME_DUPLICATE(3001, "影院名称已存在"),
    CINEMA_HAS_ACTIVE_SHOWTIMES(3002, "该影院存在在售场次，不可停用"),

    // ---- 影厅（4000-4999）----

    HALL_NOT_FOUND(4000, "影厅不存在"),
    HALL_NAME_DUPLICATE(4001, "该影院下已存在同名影厅"),

    // ---- 场次（5000-5999）----

    SHOWTIME_NOT_FOUND(5000, "场次不存在"),
    SHOWTIME_TIME_CONFLICT(5001, "该时间段与已有场次冲突"),
    SHOWTIME_HAS_LOCKED_SEATS(5002, "该场次已有锁座或订单，不可修改时间"),

    // ---- 订单（6000-6999）----

    ORDER_NOT_FOUND(6000, "订单不存在"),

    // ---- 文件（7000-7999）----

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
