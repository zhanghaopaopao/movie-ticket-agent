package com.szml.movieticket.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户状态枚举。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
@Getter
public enum UserStatus {

    INACTIVE(0, "禁用"),
    ACTIVE(1, "正常");

    @EnumValue
    private final int code;
    private final String desc;

    UserStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserStatus fromCode(int code) {
        for (UserStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return ACTIVE;
    }
}
