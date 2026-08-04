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

    /** 影厅ID */
    private Long hallId;

    /** 影厅名称 */
    private String hallName;

    /** 厅型描述 */
    private String hallType;

    /** 所属影院名称 */
    private String cinemaName;

    /** 座位统计：totalSeats/normalSeats/coupleSeats/unavailableSeats */
    private Map<String, Integer> summary;

    /** 按排组织的座位列表 */
    private List<RowVO> rows;

    @Data
    public static class RowVO {
        /** 排号 */
        private Integer rowNo;
        /** 该排的座位列表 */
        private List<SeatItemVO> seats;
    }

    @Data
    public static class SeatItemVO {
        /** 座位ID */
        private Long id;
        /** 座号 */
        private Integer seatNo;
        /** 区域：FRONT/MIDDLE/BACK/COUPLE */
        private String zone;
        /** 座位类型：NORMAL/COUPLE */
        private String seatType;
        /** 座位状态：AVAILABLE/UNAVAILABLE */
        private String status;
    }
}
