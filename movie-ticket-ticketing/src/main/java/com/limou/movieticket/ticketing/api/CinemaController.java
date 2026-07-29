package com.limou.movieticket.ticketing.api;

import com.limou.movieticket.common.api.ApiResponse;
import com.limou.movieticket.common.api.PageResult;
import com.limou.movieticket.ticketing.service.TicketingQueryService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/cinemas")
public class CinemaController {
    private final TicketingQueryService queryService;

    public CinemaController(TicketingQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ApiResponse<PageResult<CinemaSummary>> findCinemas(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String hallType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(queryService.findCinemas(city, district, brand, hallType, keyword,
                latitude, longitude, page, pageSize));
    }

    @GetMapping("/{id}/showtimes")
    public ApiResponse<List<ShowtimeSummary>> findShowtimes(
            @PathVariable String id,
            @RequestParam(required = false) String movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(queryService.findShowtimes(id, movieId, date));
    }
}
