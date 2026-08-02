package com.szml.movieticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 保存/更新草稿 DTO。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
public class DraftSaveDTO {

    /** 当前草稿版本号（乐观锁），首次创建时传 0 */
    @NotNull(message = "版本号不能为空")
    private Integer version;

    /** 影片ID */
    private Long movieId;

    /** 影院ID（传 null 表示清空） */
    private Long cinemaId;

    /** 日期时间范围 { start, end }，格式 yyyy-MM-dd'T'HH:mm:ss */
    private DateTimeRange dateTime;

    /** 场次ID（传 null 表示清空） */
    private Long showtimeId;

    /** 票数 1-6 */
    private Integer ticketCount;

    /** 预算 { perTicket, total }（单位：分） */
    private Budget budget;

    /** 座位ID数组 [101, 102] */
    private List<Long> seats;

    /** 来源：AI / TRADITIONAL */
    private String sourceMode;

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
}
