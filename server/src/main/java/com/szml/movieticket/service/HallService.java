package com.szml.movieticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szml.movieticket.dto.HallCreateDTO;
import com.szml.movieticket.dto.HallUpdateDTO;
import com.szml.movieticket.dto.SeatCreateDTO;
import com.szml.movieticket.dto.SeatLayoutSaveDTO;
import com.szml.movieticket.dto.SeatUpdateDTO;
import com.szml.movieticket.entity.Hall;
import com.szml.movieticket.vo.HallSeatVO;
import com.szml.movieticket.vo.HallVO;
import com.szml.movieticket.vo.SeatVO;

import java.util.List;

/**
 * 影厅服务接口。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
public interface HallService extends IService<Hall> {

    /**
     * 查询某影院下的所有影厅。
     */
    List<HallVO> listHallsByCinemaId(Long cinemaId);

    /**
     * 新增影厅。
     */
    HallVO createHall(HallCreateDTO dto);

    /**
     * 编辑影厅。
     */
    HallVO updateHall(Long id, HallUpdateDTO dto);

    /**
     * 查询影厅座位布局。
     */
    HallSeatVO getHallSeats(Long hallId);

    /** 新增影厅物理座位。 */
    SeatVO createSeat(Long hallId, SeatCreateDTO dto);

    /** 编辑影厅物理座位。 */
    SeatVO updateSeat(Long hallId, Long seatId, SeatUpdateDTO dto);

    /** 删除影厅物理座位。 */
    void deleteSeat(Long hallId, Long seatId);

    /** 批量保存影厅物理座位布局。 */
    HallSeatVO saveSeatLayout(Long hallId, SeatLayoutSaveDTO dto);
}
