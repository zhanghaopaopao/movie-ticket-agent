package com.szml.movieticket.controller.admin;

import com.szml.movieticket.dto.MovieCreateDTO;
import com.szml.movieticket.dto.MovieStatusDTO;
import com.szml.movieticket.dto.MovieUpdateDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.MovieService;
import com.szml.movieticket.vo.MoviePageVO;
import com.szml.movieticket.vo.MovieVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 影片管理 Controller（B 端）。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    /**
     * 分页查询影片列表。
     */
    @GetMapping
    public Result<MoviePageVO> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String status) {
        log.info("B端查询影片列表, 页码: {}, 每页条数: {}, 搜索关键词: {}, 上映状态: {}", page, size, keyword, status);
        MoviePageVO moviePageVO = movieService.pageMovies(page, size, keyword, status);
        return Result.success(moviePageVO);
    }

    /**
     * 影片详情。
     */
    @GetMapping("/{id}")
    public Result<MovieVO> detail(@PathVariable Long id) {
        log.info("查询影片详情, 影片ID: {}", id);
        MovieVO movieVO = movieService.getMovieDetail(id);
        return Result.success(movieVO);
    }

    /**
     * 新增影片。
     */
    @PostMapping
    public Result<Void> create(@Valid @RequestBody MovieCreateDTO dto) {
        log.info("新增影片, 影片名称: {}", dto.getName());
        movieService.createMovie(dto);
        return Result.success();
    }

    /**
     * 编辑影片信息（含状态）。
     */
    @PutMapping("/{id}")
    public Result<MovieVO> update(@PathVariable Long id, @RequestBody MovieUpdateDTO dto) {
        log.info("编辑影片, 影片ID: {}", id);
        MovieVO movieVO = movieService.updateMovie(id, dto);
        return Result.success(movieVO);
    }

    /**
     * 上下架影片。
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody MovieStatusDTO dto) {
        log.info("变更影片状态, 影片ID: {}, 目标状态: {}", id, dto.getStatus());
        movieService.updateMovieStatus(id, dto);
        return Result.success();
    }

    /**
     * 删除影片（有关联场次时不允许删除）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteMovie(@PathVariable Long id) {
        log.info("删除影片, 影片ID: {}", id);
        movieService.deleteMovie(id);
        return Result.success();
    }
}
