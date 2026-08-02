package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/**
 * 支付结果 VO。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
public class PayResultVO {

    private Long orderId;

    /** 订单状态：TICKETED / PAID */
    private String status;

    private Double paidAmount;

    /** 电子票（TICKETED 时有值） */
    private List<TicketItem> tickets;

    @Data
    public static class TicketItem {
        private String ticketCode;
        private String seat;
        private String qrContent;
    }
}
