package com.szml.movieticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szml.movieticket.dto.CinemaCreateDTO;
import com.szml.movieticket.dto.CinemaStatusDTO;
import com.szml.movieticket.dto.CinemaUpdateDTO;
import com.szml.movieticket.entity.Cinema;
import com.szml.movieticket.vo.CinemaOptionVO;
import com.szml.movieticket.vo.CinemaPageVO;
import com.szml.movieticket.vo.CinemaVO;

import java.util.List;

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
    void createCinema(CinemaCreateDTO dto);

    /**
     * 编辑影院。
     */
    void updateCinema(Long id, CinemaUpdateDTO dto);

    /**
     * 启停影院。
     */
    void updateCinemaStatus(Long id, CinemaStatusDTO dto);

    /**
     * 查询未停用影院及其未停用影厅的 id + name，供下拉联动使用。
     */
    List<CinemaOptionVO> listCinemaOptions();

    /**
     * C端分页查询影院列表（含 minPrice、hallTypes）。
     */
    CinemaPageVO listCinemasForUser(int page, int size, String district, String brand, String hallType, String keyword);

    /**
     * C端附近影院查询（Haversine 公式按距离排序）。
     */
    CinemaPageVO listNearbyCinemas(int page, int size, double lat, double lng, int radius);
}
