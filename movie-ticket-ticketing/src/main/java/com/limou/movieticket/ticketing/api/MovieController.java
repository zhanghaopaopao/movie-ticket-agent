package com.limou.movieticket.ticketing.api;

import com.limou.movieticket.common.api.ApiResponse;
import com.limou.movieticket.common.api.PageResult;
import com.limou.movieticket.ticketing.domain.MovieStatus;
import com.limou.movieticket.ticketing.service.TicketingQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {
    private final TicketingQueryService queryService;

    public MovieController(TicketingQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public ApiResponse<PageResult<MovieSummary>> findMovies(
            @RequestParam(defaultValue = "NOW_SHOWING") MovieStatus status,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return ApiResponse.success(queryService.findMovies(status, genre, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<MovieDetail> getMovie(@PathVariable String id) {
        return ApiResponse.success(queryService.getMovie(id));
    }
}
