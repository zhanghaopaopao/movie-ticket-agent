package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单列表 VO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    /** 用户信息 */
    private Long userId;
    private String userEmail;

    /** 影片名 */
    private String movieName;

    /** 影院名 */
    private String cinemaName;

    /** 影厅名 */
    private String hallName;

    /** 开场时间 */
    private LocalDateTime startAt;

    /** 座位摘要，如 "5排7座, 5排8座" */
    private String seatSummary;

    /** 金额（元） */
    private Double amount;

    /** 状态 */
    private String status;

    /** 状态描述 */
    private String statusDesc;

    private LocalDateTime createTime;
}
