package com.szml.movieticket.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 影片状态枚举。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Getter
public enum MovieStatus {

    COMING_SOON(0, "待上映"),
    NOW_SHOWING(1, "热映中"),
    OFFLINE(2, "已下架");

    @EnumValue
    private final int code;
    private final String desc;

    MovieStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MovieStatus fromCode(int code) {
        for (MovieStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return COMING_SOON;
    }
}
