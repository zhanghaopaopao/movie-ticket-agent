package com.szml.movieticket.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 影厅类型枚举。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Getter
public enum HallType {

    IMAX("IMAX", "IMAX"),
    DOLBY("杜比", "杜比"),
    DIGITAL("数字", "数字"),
    FOUR_DX("4DX", "4DX"),
    NORMAL("普通", "普通");

    @EnumValue
    private final String code;
    private final String desc;

    HallType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
