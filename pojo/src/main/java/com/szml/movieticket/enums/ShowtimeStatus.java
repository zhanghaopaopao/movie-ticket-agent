package com.szml.movieticket.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 场次状态枚举。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Getter
public enum ShowtimeStatus {

    SOLD_OUT(0, "停售"),
    ON_SALE(1, "在售"),
    SOLD_OUT_ALL(2, "售罄");

    @EnumValue
    private final int code;
    private final String desc;

    ShowtimeStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ShowtimeStatus fromCode(int code) {
        for (ShowtimeStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return ON_SALE;
    }
}
