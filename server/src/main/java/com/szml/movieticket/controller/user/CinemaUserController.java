package com.szml.movieticket.controller.user;

import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.CinemaService;
import com.szml.movieticket.vo.CinemaPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 影院接口（C 端）。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
@Slf4j
@RestController
@RequestMapping("/api/user/cinemas")
@RequiredArgsConstructor
public class CinemaUserController {

    private final CinemaService cinemaService;

    /**
     * 分页查询影院列表。
     */
    @GetMapping
    public Result<CinemaPageVO> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String district,
                                      @RequestParam(required = false) String brand,
                                      @RequestParam(required = false) String hallType,
                                      @RequestParam(required = false) String keyword) {
        log.info("C端查询影院列表, 页码: {}, 每页条数: {}, 所属商圈: {}, 品牌: {}, 特色厅型: {}, 搜索关键词: {}",
                page, size, district, brand, hallType, keyword);
        CinemaPageVO cinemaPageVO = cinemaService.listCinemasForUser(page, size, district, brand, hallType, keyword);
        return Result.success(cinemaPageVO);
    }

    /**
     * 附近影院查询。
     */
    @GetMapping("/nearby")
    public Result<CinemaPageVO> nearby(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam double lat,
                                        @RequestParam double lng,
                                        @RequestParam(defaultValue = "5") int radius) {
        log.info("C端附近影院查询, 页码: {}, 每页条数: {}, 用户纬度: {}, 用户经度: {}, 搜索半径(km): {}",
                page, size, lat, lng, radius);
        CinemaPageVO cinemaPageVO = cinemaService.listNearbyCinemas(page, size, lat, lng, radius);
        return Result.success(cinemaPageVO);
    }
}
