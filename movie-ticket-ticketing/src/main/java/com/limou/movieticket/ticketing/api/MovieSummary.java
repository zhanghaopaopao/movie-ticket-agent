package com.limou.movieticket.ticketing.api;

import com.limou.movieticket.ticketing.domain.MovieStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MovieSummary(String id, String name, String englishName, List<String> genres,
                           int durationMinutes, BigDecimal rating, String posterUrl, LocalDate releaseDate,
                           List<String> cast, long wantCount, MovieStatus status,
                           long cinemaCount, long showtimeCount) { }
