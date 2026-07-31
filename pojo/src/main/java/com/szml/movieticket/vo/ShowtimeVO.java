package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场次 VO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class ShowtimeVO {

    private Long id;

    /** 影片 */
    private MovieBriefVO movie;

    /** 影院 */
    private CinemaBriefVO cinema;

    /** 影厅 */
    private HallBriefVO hall;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Integer basePrice;

    private String language;

    private Integer status;

    private String statusDesc;

    /** 已售座位数 */
    private Integer soldSeats;

    /** 总座位数 */
    private Integer totalSeats;

    /** 当前锁定中的座位数 */
    private Integer lockedCount;

    private LocalDateTime createTime;

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

    @Data
    public static class HallBriefVO {
        private Long id;
        private String name;
        private String hallType;
    }
}
