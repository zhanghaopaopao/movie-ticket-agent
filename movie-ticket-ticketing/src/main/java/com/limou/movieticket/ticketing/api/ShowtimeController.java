package com.limou.movieticket.ticketing.api;

import com.limou.movieticket.common.api.ApiResponse;
import com.limou.movieticket.ticketing.service.TicketingQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowtimeController {
    private final TicketingQueryService queryService;

    public ShowtimeController(TicketingQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{id}/seats")
    public ApiResponse<SeatMapResponse> getSeatMap(@PathVariable String id) {
        return ApiResponse.success(queryService.getSeatMap(id));
    }
}
