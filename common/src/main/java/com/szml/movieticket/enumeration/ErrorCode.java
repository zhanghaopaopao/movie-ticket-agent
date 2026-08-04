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
    EMAIL_CODE_INVALID(1004, "验证码错误或已过期"),
    EMAIL_CODE_RATE_LIMIT(1005, "请60秒后再发送验证码"),
    EMAIL_SEND_FAILED(1008, "验证码邮件发送失败，请稍后重试"),
    USER_EMAIL_EXISTS(1006, "该邮箱已被注册"),
    USER_PHONE_EXISTS(1007, "该手机号已被注册"),
    USER_EMAIL_NOT_FOUND(1008, "该邮箱未注册"),
    AUTH_NOT_ADMIN(1009, "该账号无管理员权限"),

    // ---- 影片（2000-2999）----

    MOVIE_NOT_FOUND(2000, "影片不存在"),
    MOVIE_NAME_DUPLICATE(2001, "影片名称已存在"),
    MOVIE_HAS_ACTIVE_SHOWTIMES(2002, "该影片存在在售场次，不可下架"),
    MOVIE_DURATION_IMMUTABLE(2003, "该影片已关联场次，不可修改时长"),
    MOVIE_STATUS_INVALID(2005, "新增影片不能选择已下架状态"),
    MOVIE_RELEASE_DATE_PAST(2006, "热映中的影片上映日期不能晚于今天"),
    MOVIE_RELEASE_DATE_FUTURE(2007, "待上映的影片上映日期必须在今天之后"),
    MOVIE_RELEASE_DATE_AFTER_SHOWTIME(2008, "该影片存在早于新上映日期的场次，不可修改"),

    // ---- 影院（3000-3999）----

    CINEMA_NOT_FOUND(3000, "影院不存在"),
    CINEMA_NAME_DUPLICATE(3001, "影院名称已存在"),
    CINEMA_HAS_ACTIVE_SHOWTIMES(3002, "该影院存在在售场次，不可停用"),

    // ---- 影厅（4000-4999）----

    HALL_NOT_FOUND(4000, "影厅不存在"),
    HALL_NAME_DUPLICATE(4001, "该影院下已存在同名影厅"),

    // ---- 座位（4100-4199）----

    SEAT_NOT_FOUND(4100, "座位不存在"),
    SEAT_POSITION_DUPLICATE(4101, "该影厅下已存在相同排号和座号"),
    SEAT_POSITION_INVALID(4102, "座位排号和座号必须为正整数"),
    SEAT_ZONE_INVALID(4103, "座位区域不合法"),
    SEAT_TYPE_INVALID(4104, "座位类型不合法"),
    SEAT_STATUS_INVALID(4105, "座位状态不合法"),
    SEAT_HAS_ACTIVE_INVENTORY(4106, "座位存在锁定或已售库存，不可删除或调整布局"),
    SEAT_HAS_ORDER_RECORD(4107, "座位存在订单记录，不可删除或调整布局"),
    SEAT_LAYOUT_INVALID(4108, "座位布局数据不合法"),

    // ---- 场次（5000-5999）----

    SHOWTIME_NOT_FOUND(5000, "场次不存在"),
    SHOWTIME_TIME_CONFLICT(5001, "该时间段与已有场次冲突"),
    SHOWTIME_HAS_LOCKED_SEATS(5002, "该场次已有锁座或订单，不可修改时间"),
    SHOWTIME_SEAT_NOT_FOUND(5003, "场次座位不存在"),
    SHOWTIME_SEAT_STATUS_INVALID(5004, "场次座位状态不合法"),

    // ---- 订单（6000-6999）----

    ORDER_NOT_FOUND(6000, "订单不存在"),
    ORDER_EXPIRED(6001, "订单已超时，请重新选座"),
    ORDER_STATUS_INVALID(6002, "当前订单状态不允许此操作"),
    SEAT_LOCK_CONFLICT(6003, "座位已被锁定或售出"),
    PAYMENT_IDEMPOTENT_REPLAY(6004, "请勿重复支付"),

    // ---- 草稿（7000-7999）----

    DRAFT_NOT_FOUND(7000, "草稿不存在"),
    DRAFT_VERSION_CONFLICT(7001, "草稿已在另一页面更新，请确认后继续"),

    // ---- 文件（8000-8999）----

    FILE_UPLOAD_ERROR(8000, "文件上传失败"),
    FILE_FORMAT_INVALID(8001, "仅支持 jpg/png/webp 格式的图片文件"),
    FILE_SIZE_EXCEED(8002, "文件大小不能超过 10MB"),

    // ---- 用户（9000-9999）----

    USER_OLD_PASSWORD_WRONG(9000, "当前密码不正确");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
