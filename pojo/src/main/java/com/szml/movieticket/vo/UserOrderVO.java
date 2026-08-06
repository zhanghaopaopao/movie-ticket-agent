package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * C 端订单列表项 VO。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
public class UserOrderVO {

    /** 订单ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 影片名称 */
    private String movieName;

    /** 影片海报URL */
    private String poster;

    /** 影院名称 */
    private String cinemaName;

    /** 影厅名称 */
    private String hallName;

    /** 开场时间 */
    private LocalDateTime startAt;

    /** 座位摘要，如 "5排7座, 5排8座" */
    private String seatSummary;

    /** 订单金额（元） */
    private Double amount;

    /** 订单状态 */
    private String status;

    /** 订单状态描述 */
    private String statusDesc;

    /** 支付截止时间 */
    private LocalDateTime expiresAt;

    /** 创建时间 */
    private LocalDateTime createTime;
}
