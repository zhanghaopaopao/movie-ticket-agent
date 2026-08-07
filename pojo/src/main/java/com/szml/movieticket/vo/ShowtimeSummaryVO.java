package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场次摘要（Agent 用，挂在影片下展示"哪家影院何时有场次"）。
 *
 * @author zhanghao
 * @since 2026-08-07
 */
@Data
public class ShowtimeSummaryVO {

    /** 场次ID */
    private Long showtimeId;

    /** 影院名称 */
    private String cinemaName;

    /** 影厅名称 */
    private String hallName;

    /** 开映时间 */
    private LocalDateTime startAt;

    /** 票价（元） */
    private String price;

    /** 剩余座位 */
    private Integer remainingSeats;
}
