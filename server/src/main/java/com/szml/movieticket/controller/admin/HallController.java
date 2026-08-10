package com.szml.movieticket.controller.admin;

import com.szml.movieticket.dto.HallCreateDTO;
import com.szml.movieticket.dto.HallStatusDTO;
import com.szml.movieticket.dto.HallUpdateDTO;
import com.szml.movieticket.dto.SeatCreateDTO;
import com.szml.movieticket.dto.SeatLayoutSaveDTO;
import com.szml.movieticket.dto.SeatUpdateDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.HallService;
import com.szml.movieticket.vo.HallPageVO;
import com.szml.movieticket.vo.HallSeatVO;
import com.szml.movieticket.vo.HallVO;
import com.szml.movieticket.vo.SeatVO;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
     * 分页查询某影院下的影厅。
     */
    @GetMapping("/api/admin/cinemas/{cinemaId}/halls")
    public Result<HallPageVO> listByCinema(@PathVariable Long cinemaId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword) {
        log.info("查询影厅列表, 影院ID: {}, 页码: {}, 每页条数: {}, 搜索关键词: {}", cinemaId, page, size, keyword);
        HallPageVO hallPageVO = hallService.pageHallsByCinemaId(page, size, cinemaId, keyword);
        return Result.success(hallPageVO);
    }

    /**
     * 新增影厅。
     */
    @PostMapping("/api/admin/halls")
    public Result<Void> create(@Valid @RequestBody HallCreateDTO dto) {
        log.info("新增影厅, 影厅名称: {}, 影院ID: {}", dto.getName(), dto.getCinemaId());
        hallService.createHall(dto);
        return Result.success();
    }

    /**
     * 编辑影厅。
     */
    @PutMapping("/api/admin/halls/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody HallUpdateDTO dto) {
        log.info("编辑影厅, 影厅ID: {}", id);
        hallService.updateHall(id, dto);
        return Result.success();
    }

    /**
     * 启停影厅。
     */
    @PutMapping("/api/admin/halls/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody HallStatusDTO dto) {
        log.info("变更影厅状态, 影厅ID: {}, 目标状态: {}", id, dto.getStatus());
        hallService.updateHallStatus(id, dto);
        return Result.success();
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
     * 批量新增影厅物理座位。
     */
    @PostMapping("/api/admin/halls/{hallId}/seats/batch")
    public Result<Void> createSeatsBatch(@PathVariable Long hallId,
                                         @Valid @RequestBody List<SeatCreateDTO> dtos) {
        log.info("批量新增物理座位, hallId: {}, count: {}", hallId, dtos.size());
        hallService.batchCreateSeats(hallId, dtos);
        return Result.success();
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
    @DeleteMapping("/api/admin/halls/{hallId}/seats/batch")
    public Result<Void> deleteSeatsBatch(@PathVariable Long hallId,
                                         @RequestBody List<Long> seatIds) {
        log.info("批量删除物理座位, hallId: {}, count: {}", hallId, seatIds.size());
        hallService.batchDeleteSeats(hallId, seatIds);
        return Result.success();
    }

//    @DeleteMapping("/api/admin/halls/{hallId}/seats/{seatId}")
//    public Result<Void> deleteSeat(@PathVariable Long hallId,
//                                   @PathVariable Long seatId) {
//        log.info("删除物理座位, hallId: {}, seatId: {}", hallId, seatId);
//        hallService.deleteSeat(hallId, seatId);
//        return Result.success();
//    }

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
