package com.szml.movieticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szml.movieticket.dto.MovieCreateDTO;
import com.szml.movieticket.dto.MovieStatusDTO;
import com.szml.movieticket.dto.MovieUpdateDTO;
import com.szml.movieticket.entity.Movie;
import com.szml.movieticket.vo.MoviePageVO;
import com.szml.movieticket.vo.MovieVO;

import java.util.Map;

/**
 * 影片服务接口。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
public interface MovieService extends IService<Movie> {

    /**
     * 分页查询影片列表。
     */
    MoviePageVO pageMovies(int page, int size, String keyword, String status);

    /**
     * 查询影片详情。
     */
    MovieVO getMovieDetail(Long id);

    /**
     * 新增影片。
     */
    void createMovie(MovieCreateDTO dto);

    /**
     * 编辑影片。
     */
    MovieVO updateMovie(Long id, MovieUpdateDTO dto);

    /**
     * 上下架影片。
     */
    void updateMovieStatus(Long id, MovieStatusDTO dto);

    /**
     * 删除影片（有关联场次时不允许删除）。
     */
    void deleteMovie(Long id);

    /**
     * C端分页查询影片列表（含 showtimeCount、cinemaCount 统计）。
     */
    MoviePageVO listMoviesForUser(int page, int size, String status, String genre, String keyword,
                                  String sortBy, String sortOrder);

    /**
     * C端影片详情（含 todayShowtimeCoverage）。
     */
    MovieVO getMovieDetailForUser(Long id);
}
