package com.szml.movieticket.controller.user;

import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.MovieService;
import com.szml.movieticket.vo.MoviePageVO;
import com.szml.movieticket.vo.MovieVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 影片接口（C 端）。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
@Slf4j
@RestController
@RequestMapping("/api/user/movies")
@RequiredArgsConstructor
public class MovieUserController {

    private final MovieService movieService;

    /**
     * 分页查询影片列表。
     */
    @GetMapping
    public Result<MoviePageVO> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String genre,
                                     @RequestParam(required = false) String keyword) {
        log.info("C端查询影片列表, 页码: {}, 每页条数: {}, 上映状态: {}, 影片类型: {}, 搜索关键词: {}",
                page, size, status, genre, keyword);
        MoviePageVO moviePageVO = movieService.listMoviesForUser(page, size, status, genre, keyword);
        return Result.success(moviePageVO);
    }

    /**
     * 影片详情。
     */
    @GetMapping("/{id}")
    public Result<MovieVO> detail(@PathVariable Long id) {
        log.info("C端查询影片详情, 影片ID: {}", id);
        MovieVO movieVO = movieService.getMovieDetailForUser(id);
        return Result.success(movieVO);
    }
}
