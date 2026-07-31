package com.szml.movieticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szml.movieticket.dto.HallCreateDTO;
import com.szml.movieticket.dto.HallUpdateDTO;
import com.szml.movieticket.entity.Hall;
import com.szml.movieticket.vo.HallSeatVO;
import com.szml.movieticket.vo.HallVO;

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
}
