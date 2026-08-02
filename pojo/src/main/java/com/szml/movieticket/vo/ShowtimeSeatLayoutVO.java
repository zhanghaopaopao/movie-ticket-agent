package com.szml.movieticket.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 某个场次的座位库存布局。
 */
@Data
public class ShowtimeSeatLayoutVO {

    private Long showtimeId;
    private String movieName;
    private String cinemaName;
    private String hallName;
    private String hallType;
    private LocalDateTime startAt;
    private Integer basePrice;
    private Integer totalSeats;
    private Integer availableSeats;
    private Integer lockedSeats;
    private Integer soldSeats;
    private Integer unavailableSeats;
    private List<RowVO> rows;

    @Data
    public static class RowVO {
        private Integer rowNo;
        private List<SeatVO> seats;
    }

    @Data
    public static class SeatVO {
        /** showtime_seat.id，用于修改场次库存状态 */
        private Long id;
        /** seat.id，物理座位主键 */
        private Long physicalSeatId;
        private Integer seatNo;
        private String zone;
        private String seatType;
        /** AVAILABLE / LOCKED / SOLD / UNAVAILABLE / COUPLE */
        private String status;
        private Integer price;
    }
}
