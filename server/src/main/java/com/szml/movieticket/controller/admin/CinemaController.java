package com.szml.movieticket.controller.admin;

import com.szml.movieticket.dto.CinemaCreateDTO;
import com.szml.movieticket.dto.CinemaStatusDTO;
import com.szml.movieticket.dto.CinemaUpdateDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.CinemaService;
import com.szml.movieticket.vo.CinemaPageVO;
import com.szml.movieticket.vo.CinemaVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 影院管理 Controller（B 端）。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/cinemas")
@RequiredArgsConstructor
public class CinemaController {

    private final CinemaService cinemaService;

    /**
     * 分页查询影院列表。
     */
    @GetMapping
    public Result<CinemaPageVO> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String district,
                                      @RequestParam(required = false) Integer status) {
        log.info("B端查询影院列表, 页码: {}, 每页条数: {}, 搜索关键词: {}, 所属商圈: {}, 启用状态: {}",
                page, size, keyword, district, status);
        CinemaPageVO cinemaPageVO = cinemaService.pageCinemas(page, size, keyword, district, status);
        return Result.success(cinemaPageVO);
    }

    /**
     * 影院详情。
     */
    @GetMapping("/{id}")
    public Result<CinemaVO> detail(@PathVariable Long id) {
        log.info("查询影院详情, 影院ID: {}", id);
        CinemaVO cinemaVO = cinemaService.getCinemaDetail(id);
        return Result.success(cinemaVO);
    }

    /**
     * 新增影院。
     */
    @PostMapping
    public Result<CinemaVO> create(@Valid @RequestBody CinemaCreateDTO dto) {
        log.info("新增影院, 影院名称: {}", dto.getName());
        CinemaVO cinemaVO = cinemaService.createCinema(dto);
        return Result.success(cinemaVO);
    }

    /**
     * 编辑影院。
     */
    @PutMapping("/{id}")
    public Result<CinemaVO> update(@PathVariable Long id, @RequestBody CinemaUpdateDTO dto) {
        log.info("编辑影院, 影院ID: {}", id);
        CinemaVO cinemaVO = cinemaService.updateCinema(id, dto);
        return Result.success(cinemaVO);
    }

    /**
     * 启停影院。
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody CinemaStatusDTO dto) {
        log.info("变更影院状态, 影院ID: {}, 目标状态: {}", id, dto.getStatus());
        cinemaService.updateCinemaStatus(id, dto);
        return Result.success();
    }
}
