package com.szml.movieticket.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 影厅状态枚举。
 *
 * @author zhanghao
 * @since 2026-08-04
 */
@Getter
public enum HallStatus {

    INACTIVE(0, "停用"),
    ACTIVE(1, "启用");

    @EnumValue
    private final int code;
    private final String desc;

    HallStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static HallStatus fromCode(int code) {
        for (HallStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return ACTIVE;
    }
}
