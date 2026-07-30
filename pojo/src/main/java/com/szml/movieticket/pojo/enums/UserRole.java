package com.szml.movieticket.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户角色枚举。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
@Getter
public enum UserRole {

    USER(0, "普通用户"),
    ADMIN(1, "管理员");

    @EnumValue
    private final int code;
    private final String desc;

    UserRole(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserRole fromCode(int code) {
        for (UserRole value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return USER;
    }
}
