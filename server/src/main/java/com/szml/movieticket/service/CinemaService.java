package com.szml.movieticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szml.movieticket.dto.CinemaCreateDTO;
import com.szml.movieticket.dto.CinemaStatusDTO;
import com.szml.movieticket.dto.CinemaUpdateDTO;
import com.szml.movieticket.entity.Cinema;
import com.szml.movieticket.vo.CinemaPageVO;
import com.szml.movieticket.vo.CinemaVO;

/**
 * 影院服务接口。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
public interface CinemaService extends IService<Cinema> {

    /**
     * 分页查询影院列表。
     */
    CinemaPageVO pageCinemas(int page, int size, String keyword, String district, Integer status);

    /**
     * 查询影院详情。
     */
    CinemaVO getCinemaDetail(Long id);

    /**
     * 新增影院。
     */
    CinemaVO createCinema(CinemaCreateDTO dto);

    /**
     * 编辑影院。
     */
    CinemaVO updateCinema(Long id, CinemaUpdateDTO dto);

    /**
     * 启停影院。
     */
    CinemaVO updateCinemaStatus(Long id, CinemaStatusDTO dto);
}
