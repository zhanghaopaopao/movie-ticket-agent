package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/**
 * 购票草稿 VO。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
public class DraftVO {

    private Long id;

    /** 版本号 */
    private Integer version;

    /** 草稿状态 */
    private String status;

    /** 来源模式 */
    private String sourceMode;

    /** 影片 */
    private Brief movie;

    /** 影院 */
    private Brief cinema;

    /** 日期时间范围 */
    private DateTimeRange dateTime;

    /** 场次 */
    private Brief showtime;

    /** 票数 */
    private Integer ticketCount;

    /** 预算 */
    private Budget budget;

    /** 已选座位 */
    private List<SeatItem> seats;

    /** 级联清除的字段 */
    private List<String> clearedFields;

    /** 是否可进入选座 */
    private Boolean canProceedToSeat;

    /** 关联订单ID */
    private Long orderId;

    @Data
    public static class Brief {
        private Long id;
        private String name;
        private String poster;
    }

    @Data
    public static class DateTimeRange {
        private String start;
        private String end;
    }

    @Data
    public static class Budget {
        private Integer perTicket;
        private Integer total;
    }

    @Data
    public static class SeatItem {
        private Integer rowNo;
        private Integer seatNo;
    }
}
