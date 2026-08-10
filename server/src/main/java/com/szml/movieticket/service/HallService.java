package com.szml.movieticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szml.movieticket.dto.HallCreateDTO;
import com.szml.movieticket.dto.HallStatusDTO;
import com.szml.movieticket.dto.HallUpdateDTO;
import com.szml.movieticket.dto.SeatCreateDTO;
import com.szml.movieticket.dto.SeatLayoutSaveDTO;
import com.szml.movieticket.dto.SeatUpdateDTO;
import com.szml.movieticket.entity.Hall;
import com.szml.movieticket.vo.HallPageVO;
import com.szml.movieticket.vo.HallSeatVO;
import com.szml.movieticket.vo.HallVO;
import com.szml.movieticket.vo.SeatVO;

/**
 * 影厅服务接口。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
public interface HallService extends IService<Hall> {

    /**
     * 分页查询某影院下的影厅。
     */
    HallPageVO pageHallsByCinemaId(int page, int size, Long cinemaId, String keyword);

    /**
     * 新增影厅。
     */
    void createHall(HallCreateDTO dto);

    /**
     * 编辑影厅。
     */
    void updateHall(Long id, HallUpdateDTO dto);

    /**
     * 启停影厅。
     */
    void updateHallStatus(Long id, HallStatusDTO dto);

    /**
     * 查询影厅座位布局。
     */
    HallSeatVO getHallSeats(Long hallId);

    /** 新增影厅物理座位。 */
    SeatVO createSeat(Long hallId, SeatCreateDTO dto);

    /** 批量新增影厅物理座位，同一事务。 */
    void batchCreateSeats(Long hallId, java.util.List<SeatCreateDTO> dtos);

    /** 编辑影厅物理座位。 */
    SeatVO updateSeat(Long hallId, Long seatId, SeatUpdateDTO dto);

    /** 删除影厅物理座位。 */
    void deleteSeat(Long hallId, Long seatId);

    /** 批量删除影厅物理座位。 */
    void batchDeleteSeats(Long hallId, java.util.List<Long> seatIds);

    /** 批量保存影厅物理座位布局。 */
    HallSeatVO saveSeatLayout(Long hallId, SeatLayoutSaveDTO dto);
}
