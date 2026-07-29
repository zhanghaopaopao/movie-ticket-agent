package com.limou.movieticket.ticketing.api;

import com.limou.movieticket.ticketing.domain.SeatInventoryStatus;
import com.limou.movieticket.ticketing.domain.SeatType;
import java.time.OffsetDateTime;
import java.util.List;

public record SeatMapResponse(String showtimeId, String movieId, String movieName, String cinemaId,
                              String cinemaName, String hallId, String hallName, String hallType,
                              OffsetDateTime startAt, int inventoryVersion, List<SeatView> seats) {
    public record SeatView(String id, int rowNo, int seatNo, String zone, SeatType seatType,
                           String coupleGroup, int price, SeatInventoryStatus status, int version) { }
}
