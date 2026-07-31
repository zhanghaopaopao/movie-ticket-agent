package com.szml.movieticket.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 影院状态枚举。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Getter
public enum CinemaStatus {

    INACTIVE(0, "停用"),
    ACTIVE(1, "启用");

    @EnumValue
    private final int code;
    private final String desc;

    CinemaStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CinemaStatus fromCode(int code) {
        for (CinemaStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return ACTIVE;
    }
}
