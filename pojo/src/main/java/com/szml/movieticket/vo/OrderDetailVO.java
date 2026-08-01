package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情 VO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class OrderDetailVO {

    private Long id;

    private String orderNo;

    /** 用户 */
    private Long userId;
    private String userEmail;

    /** 影片 */
    private String movieName;
    private String moviePoster;

    /** 影院 */
    private String cinemaName;
    private String cinemaAddress;

    /** 影厅 */
    private String hallName;
    private String hallType;

    /** 语言 */
    private String language;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    /** 座位明细 */
    private List<OrderItemVO> items;

    /** 金额（元） */
    private Double amount;

    private String status;
    private String statusDesc;

    /** 支付信息 */
    private PaymentInfoVO payment;

    /** 电子票 */
    private List<TicketInfoVO> tickets;

    /** 锁座审计 */
    private List<LockLogVO> seatLockLogs;

    private LocalDateTime expiresAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Data
    public static class OrderItemVO {
        private Integer rowNo;
        private Integer seatNo;
        private String zone;
        private Double unitPrice;
        private String ticketCode;
    }

    @Data
    public static class PaymentInfoVO {
        private String status;
        private Double amount;
        private String idempotencyKey;
        private LocalDateTime processedAt;
    }

    @Data
    public static class TicketInfoVO {
        private String ticketCode;
        private Integer rowNo;
        private Integer seatNo;
        private String qrContent;
    }

    @Data
    public static class LockLogVO {
        private String action;
        private Long seatId;
        private LocalDateTime createTime;
    }
}
