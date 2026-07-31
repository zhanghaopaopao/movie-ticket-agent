package com.szml.movieticket.controller.admin;

import com.szml.movieticket.dto.HallCreateDTO;
import com.szml.movieticket.dto.HallUpdateDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.HallService;
import com.szml.movieticket.vo.HallSeatVO;
import com.szml.movieticket.vo.HallVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 影厅管理 Controller（B 端）。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HallController {

    private final HallService hallService;

    /**
     * 查询某影院下的所有影厅。
     */
    @GetMapping("/api/admin/cinemas/{cinemaId}/halls")
    public Result<List<HallVO>> listByCinema(@PathVariable Long cinemaId) {
        log.info("查询影厅列表, cinemaId: {}", cinemaId);
        List<HallVO> hallVOList = hallService.listHallsByCinemaId(cinemaId);
        return Result.success(hallVOList);
    }

    /**
     * 新增影厅。
     */
    @PostMapping("/api/admin/halls")
    public Result<HallVO> create(@Valid @RequestBody HallCreateDTO dto) {
        log.info("新增影厅, name: {}, cinemaId: {}", dto.getName(), dto.getCinemaId());
        HallVO hallVO = hallService.createHall(dto);
        return Result.success(hallVO);
    }

    /**
     * 编辑影厅。
     */
    @PutMapping("/api/admin/halls/{id}")
    public Result<HallVO> update(@PathVariable Long id, @RequestBody HallUpdateDTO dto) {
        log.info("编辑影厅, id: {}", id);
        HallVO hallVO = hallService.updateHall(id, dto);
        return Result.success(hallVO);
    }

    /**
     * 查询影厅座位布局。
     */
    @GetMapping("/api/admin/halls/{hallId}/seats")
    public Result<HallSeatVO> seats(@PathVariable Long hallId) {
        log.info("查询影厅座位布局, hallId: {}", hallId);
        HallSeatVO hallSeatVO = hallService.getHallSeats(hallId);
        return Result.success(hallSeatVO);
    }
}
