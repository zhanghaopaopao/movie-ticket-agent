package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * C 端订单详情 VO。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
public class UserOrderDetailVO {

    private Long id;

    private String orderNo;

    /** 影片信息 */
    private MovieBrief movie;

    /** 影院信息 */
    private CinemaBrief cinema;

    /** 影厅名 */
    private String hallName;

    /** 影厅类型 */
    private String hallType;

    /** 语言格式 */
    private String language;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    /** 座位明细 */
    private List<OrderItemInfo> items;

    /** 订单中的零食快照。 */
    private List<SnackOrderItemVO> snacks;

    /** 零食金额，单位元。 */
    private Double snackAmount;

    private Double amount;

    private String status;

    private String statusDesc;

    /** 支付信息 */
    private PaymentInfo payment;

    /** 电子票（TICKETED 时有值） */
    private List<TicketInfo> tickets;

    private LocalDateTime expiresAt;

    private LocalDateTime createTime;

    @Data
    public static class MovieBrief {
        private Long id;
        private String name;
        private String poster;
    }

    @Data
    public static class CinemaBrief {
        private Long id;
        private String name;
        private String address;
    }

    @Data
    public static class OrderItemInfo {
        private Integer rowNo;
        private Integer seatNo;
        private String zone;
        private Double unitPrice;
        private String ticketCode;
    }

    @Data
    public static class PaymentInfo {
        private String status;
        private Double amount;
        private LocalDateTime processedAt;
    }

    @Data
    public static class TicketInfo {
        private String ticketCode;
        private Integer rowNo;
        private Integer seatNo;
        private String qrContent;
    }
}
