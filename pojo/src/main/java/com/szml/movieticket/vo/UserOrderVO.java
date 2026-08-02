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

    private Long id;

    private String orderNo;

    private String movieName;

    private String cinemaName;

    private String hallName;

    private LocalDateTime startAt;

    /** 座位摘要，如 "5排7座, 5排8座" */
    private String seatSummary;

    private Double amount;

    private String status;

    private String statusDesc;

    private LocalDateTime expiresAt;

    private LocalDateTime createTime;
}
