package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 影厅座位布局 VO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class HallSeatVO {

    private Long hallId;

    private String hallName;

    private String hallType;

    private String cinemaName;

    /** 座位统计 */
    private Map<String, Integer> summary;

    /** 按排组织的座位列表 */
    private List<RowVO> rows;

    @Data
    public static class RowVO {
        private Integer rowNo;
        private List<SeatItemVO> seats;
    }

    @Data
    public static class SeatItemVO {
        private Long id;
        private Integer seatNo;
        private String zone;
        /** NORMAL / COUPLE */
        private String seatType;
        /** AVAILABLE / UNAVAILABLE */
        private String status;
    }
}
