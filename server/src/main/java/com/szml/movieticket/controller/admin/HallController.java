package com.szml.movieticket.controller.admin;

import com.szml.movieticket.dto.HallCreateDTO;
import com.szml.movieticket.dto.HallUpdateDTO;
import com.szml.movieticket.dto.SeatCreateDTO;
import com.szml.movieticket.dto.SeatLayoutSaveDTO;
import com.szml.movieticket.dto.SeatUpdateDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.HallService;
import com.szml.movieticket.vo.HallSeatVO;
import com.szml.movieticket.vo.HallVO;
import com.szml.movieticket.vo.SeatVO;
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
        log.info("查询影厅列表, 影院ID: {}", cinemaId);
        List<HallVO> hallVOList = hallService.listHallsByCinemaId(cinemaId);
        return Result.success(hallVOList);
    }

    /**
     * 新增影厅。
     */
    @PostMapping("/api/admin/halls")
    public Result<HallVO> create(@Valid @RequestBody HallCreateDTO dto) {
        log.info("新增影厅, 影厅名称: {}, 影院ID: {}", dto.getName(), dto.getCinemaId());
        HallVO hallVO = hallService.createHall(dto);
        return Result.success(hallVO);
    }

    /**
     * 编辑影厅。
     */
    @PutMapping("/api/admin/halls/{id}")
    public Result<HallVO> update(@PathVariable Long id, @RequestBody HallUpdateDTO dto) {
        log.info("编辑影厅, 影厅ID: {}", id);
        HallVO hallVO = hallService.updateHall(id, dto);
        return Result.success(hallVO);
    }

    /**
     * 查询影厅座位布局。
     */
    @GetMapping("/api/admin/halls/{hallId}/seats")
    public Result<HallSeatVO> seats(@PathVariable Long hallId) {
        log.info("查询影厅座位布局, 影厅ID: {}", hallId);
        HallSeatVO hallSeatVO = hallService.getHallSeats(hallId);
        return Result.success(hallSeatVO);
    }

    /**
     * 新增影厅物理座位。
     */
    @PostMapping("/api/admin/halls/{hallId}/seats")
    public Result<SeatVO> createSeat(@PathVariable Long hallId,
                                     @Valid @RequestBody SeatCreateDTO dto) {
        log.info("新增物理座位, hallId: {}, rowNo: {}, seatNo: {}", hallId, dto.getRowNo(), dto.getSeatNo());
        return Result.success(hallService.createSeat(hallId, dto));
    }

    /**
     * 编辑影厅物理座位。
     */
    @PutMapping("/api/admin/halls/{hallId}/seats/{seatId}")
    public Result<SeatVO> updateSeat(@PathVariable Long hallId,
                                     @PathVariable Long seatId,
                                     @Valid @RequestBody SeatUpdateDTO dto) {
        log.info("编辑物理座位, hallId: {}, seatId: {}", hallId, seatId);
        return Result.success(hallService.updateSeat(hallId, seatId, dto));
    }

    /**
     * 删除影厅物理座位。
     */
    @DeleteMapping("/api/admin/halls/{hallId}/seats/{seatId}")
    public Result<Void> deleteSeat(@PathVariable Long hallId,
                                   @PathVariable Long seatId) {
        log.info("删除物理座位, hallId: {}, seatId: {}", hallId, seatId);
        hallService.deleteSeat(hallId, seatId);
        return Result.success();
    }

    /**
     * 批量保存影厅物理座位布局。
     */
    @PutMapping("/api/admin/halls/{hallId}/seats/layout")
    public Result<HallSeatVO> saveSeatLayout(@PathVariable Long hallId,
                                             @Valid @RequestBody SeatLayoutSaveDTO dto) {
        log.info("批量保存座位布局, hallId: {}, seatCount: {}", hallId,
                dto.getSeats() == null ? 0 : dto.getSeats().size());
        return Result.success(hallService.saveSeatLayout(hallId, dto));
    }
}
