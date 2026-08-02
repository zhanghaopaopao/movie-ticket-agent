package com.szml.movieticket.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额工具类，分 ↔ 元转换。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
public class AmountUtil {

    private AmountUtil() {}

    /**
     * 分转元（int）。
     */
    public static double yuan(int cents) {
        return BigDecimal.valueOf(cents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 分转元（long）。
     */
    public static double yuan(long cents) {
        return BigDecimal.valueOf(cents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
