package com.szml.movieticket.controller.user;

import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.ShowtimeService;
import com.szml.movieticket.vo.ShowtimeGroupedVO;
import com.szml.movieticket.vo.ShowtimeSeatLayoutVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 场次接口（C 端）。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
@Slf4j
@RestController
@RequestMapping("/api/user/showtimes")
@RequiredArgsConstructor
public class ShowtimeUserController {

    private final ShowtimeService showtimeService;

    /**
     * C端场次列表查询（按影片分组，含影院信息、剩余座位）。
     */
    @GetMapping
    public Result<ShowtimeGroupedVO> list(@RequestParam(required = false) Long movieId,
                                           @RequestParam(required = false) Long cinemaId,
                                           @RequestParam(required = false) String date,
                                           @RequestParam(required = false) String hallType) {
        log.info("C端查询场次列表, 影片ID: {}, 影院ID: {}, 日期: {}, 特色厅型: {}", movieId, cinemaId, date, hallType);
        ShowtimeGroupedVO showtimeGroupedVO = showtimeService.listShowtimesForUser(movieId, cinemaId, date, hallType);
        return Result.success(showtimeGroupedVO);
    }

    /**
     * C端查询场次座位图（含惰性过期检查）。
     */
    @GetMapping("/{id}/seats")
    public Result<ShowtimeSeatLayoutVO> seats(@PathVariable Long id) {
        log.info("C端查询座位图, 场次ID: {}", id);
        ShowtimeSeatLayoutVO showtimeSeatLayoutVO = showtimeService.getSeatLayoutForUser(id);
        return Result.success(showtimeSeatLayoutVO);
    }
}
