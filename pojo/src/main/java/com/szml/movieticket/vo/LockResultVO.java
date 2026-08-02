package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 锁座结果 VO。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
public class LockResultVO {

    private Long orderId;

    private String orderNo;

    /** 金额（元） */
    private Double amount;

    /** 支付截止时间 */
    private LocalDateTime expiresAt;

    /** 剩余秒数 */
    private Long remainingSeconds;

    /** 影片 */
    private MovieBriefVO movie;

    /** 影院 */
    private CinemaBriefVO cinema;

    /** 影厅名 */
    private String hallName;

    /** 开场时间 */
    private LocalDateTime startAt;

    /** 座位明细 */
    private List<SeatInfo> seats;

    @Data
    public static class SeatInfo {
        private Integer rowNo;
        private Integer seatNo;
        private Double price;
    }

    @Data
    public static class MovieBriefVO {
        private Long id;
        private String name;
    }

    @Data
    public static class CinemaBriefVO {
        private Long id;
        private String name;
    }
}
