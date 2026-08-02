package com.szml.movieticket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * C 端场次列表响应 VO（按影片分组）。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
@Data
public class ShowtimeGroupedVO {

    /** 影院信息 */
    private CinemaBrief cinema;

    /** 按影片分组的场次列表 */
    private List<MovieGroup> movies;

    @Data
    public static class CinemaBrief {
        private Long id;
        private String name;
        private String address;
        /** 距离（km），仅附近影院模式时有值 */
        private Double distance;
        private List<String> services;
    }

    @Data
    public static class MovieGroup {
        private Long id;
        private String name;
        private String poster;
        private Integer duration;
        private List<ShowtimeItem> showtimes;
    }

    @Data
    public static class ShowtimeItem {
        private Long id;
        private LocalDateTime startAt;
        private LocalDateTime endAt;
        private String language;
        private String hallType;
        private String hallName;
        private Integer basePrice;
        /** 剩余可选座位 */
        private Integer remainingSeats;
        /** 总座位数 */
        private Integer totalSeats;
        private String status;
    }
}
