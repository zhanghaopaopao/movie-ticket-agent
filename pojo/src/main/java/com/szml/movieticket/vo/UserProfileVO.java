package com.szml.movieticket.vo;

import lombok.Data;

/**
 * 个人中心首页 VO。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
public class UserProfileVO {

    private String phone;

    private String email;

    /** 统计 */
    private UserStats stats;

    /** 观影偏好 */
    private PreferenceVO preference;

    @Data
    public static class UserStats {
        /** 历史总订单数 */
        private long totalOrders;
        /** 已支付金额合计（元） */
        private double totalSpent;
    }

    @Data
    public static class PreferenceVO {
        private String district;
        private String hallType;
        /** 预算（元） */
        private Double budget;
        /** 预算（分） */
        private Integer budgetRaw;
        private String seatZone;
    }
}
