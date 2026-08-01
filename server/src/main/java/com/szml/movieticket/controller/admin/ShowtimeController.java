package com.szml.movieticket.controller.admin;

import com.szml.movieticket.dto.ShowtimeCreateDTO;
import com.szml.movieticket.dto.ShowtimeSeatStatusDTO;
import com.szml.movieticket.dto.ShowtimeStatusDTO;
import com.szml.movieticket.dto.ShowtimeUpdateDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.ShowtimeService;
import com.szml.movieticket.vo.ShowtimePageVO;
import com.szml.movieticket.vo.ShowtimeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 场次管理 Controller（B 端）。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    /**
     * 分页查询场次列表。
     */
    @GetMapping
    public Result<ShowtimePageVO> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(required = false) Long movieId,
                                        @RequestParam(required = false) Long cinemaId,
                                        @RequestParam(required = false) String date,
                                        @RequestParam(required = false) String status) {
        ShowtimePageVO showtimePageVO = showtimeService.pageShowtimes(page, size, movieId, cinemaId, date, status);
        return Result.success(showtimePageVO);
    }

    /**
     * 新增场次。
     */
    @PostMapping
    public Result<ShowtimeVO> create(@Valid @RequestBody ShowtimeCreateDTO dto) {
        log.info("新增场次, movieId: {}, hallId: {}, startAt: {}", dto.getMovieId(), dto.getHallId(), dto.getStartAt());
        ShowtimeVO showtimeVO = showtimeService.createShowtime(dto);
        return Result.success(showtimeVO);
    }

    /**
     * 编辑场次。
     */
    @PutMapping("/{id}")
    public Result<ShowtimeVO> update(@PathVariable Long id, @RequestBody ShowtimeUpdateDTO dto) {
        log.info("编辑场次, id: {}", id);
        ShowtimeVO showtimeVO = showtimeService.updateShowtime(id, dto);
        return Result.success(showtimeVO);
    }

    /**
     * 批量设置场次座位状态。
     */
    @PutMapping("/{id}/seats/status")
    public Result<Map<String, Object>> updateSeatStatus(@PathVariable Long id,
                                                         @Valid @RequestBody ShowtimeSeatStatusDTO dto) {
        log.info("批量设置座位状态, showtimeId: {}, targetStatus: {}, count: {}", id, dto.getStatus(), dto.getSeatIds().size());
        Map<String, Object> result = showtimeService.updateSeatStatus(id, dto);
        return Result.success(result);
    }

    /**
     * 停售/取消场次。
     */
    @PutMapping("/{id}/status")
    public Result<ShowtimeVO> updateStatus(@PathVariable Long id, @Valid @RequestBody ShowtimeStatusDTO dto) {
        log.info("变更场次状态, id: {}, targetStatus: {}", id, dto.getStatus());
        ShowtimeVO showtimeVO = showtimeService.updateShowtimeStatus(id, dto);
        return Result.success(showtimeVO);
    }
}
