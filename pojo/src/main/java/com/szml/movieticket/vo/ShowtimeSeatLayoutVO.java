package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 某个场次的座位库存布局。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class ShowtimeSeatLayoutVO {

    /** 场次ID */
    private Long showtimeId;

    /** 影片名称 */
    private String movieName;

    /** 影院名称 */
    private String cinemaName;

    /** 影厅名称 */
    private String hallName;

    /** 厅型描述 */
    private String hallType;

    /** 开场时间 */
    private LocalDateTime startAt;

    /** 基准票价（分） */
    private Integer basePrice;

    /** 总座位数 */
    private Integer totalSeats;

    /** 可选座位数 */
    private Integer availableSeats;

    /** 已锁定座位数 */
    private Integer lockedSeats;

    /** 已售座位数 */
    private Integer soldSeats;

    /** 不可用座位数 */
    private Integer unavailableSeats;

    /** 按排组织的座位列表 */
    private List<RowVO> rows;

    @Data
    public static class RowVO {
        /** 排号 */
        private Integer rowNo;
        /** 该排的座位列表 */
        private List<SeatVO> seats;
    }

    @Data
    public static class SeatVO {
        /** showtime_seat.id，用于修改场次库存状态 */
        private Long id;
        /** seat.id，物理座位主键 */
        private Long physicalSeatId;
        /** 座号 */
        private Integer seatNo;
        /** 区域：FRONT/MIDDLE/BACK/COUPLE */
        private String zone;
        /** 座位类型：NORMAL/COUPLE */
        private String seatType;
        /** 库存状态：AVAILABLE/LOCKED/SOLD/UNAVAILABLE/COUPLE */
        private String status;
        /** 售价（分） */
        private Integer price;
    }
}
