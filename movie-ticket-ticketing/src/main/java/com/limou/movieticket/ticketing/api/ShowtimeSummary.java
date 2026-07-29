package com.limou.movieticket.ticketing.api;

import com.limou.movieticket.ticketing.domain.ShowtimeStatus;
import java.time.OffsetDateTime;

public record ShowtimeSummary(String id, String movieId, String movieName, String hallId, String hallName,
                              String hallType, OffsetDateTime startAt, OffsetDateTime endAt,
                              int price, String language, String format, long remainingSeats,
                              ShowtimeStatus status) { }
