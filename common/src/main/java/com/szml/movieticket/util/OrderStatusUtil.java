package com.szml.movieticket.util;

/**
 * 订单状态工具类。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
public class OrderStatusUtil {

    private OrderStatusUtil() {}

    /**
     * 订单状态英文 → 中文。
     */
    public static String statusDesc(String status) {
        return switch (status) {
            case "PAYMENT_PENDING" -> "待支付";
            case "PAID" -> "已支付";
            case "TICKETED" -> "已出票";
            case "CANCELLED" -> "已取消";
            case "EXPIRED" -> "已过期";
            case "REFUND_PENDING" -> "退款处理中";
            case "REFUNDED" -> "已退票";
            default -> status;
        };
    }
}
