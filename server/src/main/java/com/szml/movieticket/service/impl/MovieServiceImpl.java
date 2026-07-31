package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.dto.MovieCreateDTO;
import com.szml.movieticket.dto.MovieStatusDTO;
import com.szml.movieticket.dto.MovieUpdateDTO;
import com.szml.movieticket.entity.Movie;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.enums.MovieStatus;
import com.szml.movieticket.exception.MovieException;
import com.szml.movieticket.mapper.MovieMapper;
import com.szml.movieticket.service.MovieService;
import com.szml.movieticket.vo.MoviePageVO;
import com.szml.movieticket.vo.MovieVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 影片服务实现类。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@Service
public class MovieServiceImpl extends ServiceImpl<MovieMapper, Movie> implements MovieService {

    @Override
    public MoviePageVO pageMovies(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<Movie> wrapper = buildQueryWrapper(keyword, status);
        wrapper.orderByDesc(Movie::getCreateTime);

        Page<Movie> pageResult = page(new Page<>(page, size), wrapper);
        List<MovieVO> records = pageResult.getRecords().stream().map(this::toVO).collect(Collectors.toList());

        MoviePageVO pageVO = new MoviePageVO();
        pageVO.setTotal(pageResult.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setRecords(records);
        return pageVO;
    }

    @Override
    public MovieVO getMovieDetail(Long id) {
        Movie movie = getById(id);
        if (movie == null) {
            throw new MovieException(ErrorCode.MOVIE_NOT_FOUND);
        }
        return toVO(movie);
    }

    @Override
    public MovieVO createMovie(MovieCreateDTO dto) {
        long count = count(new LambdaQueryWrapper<Movie>().eq(Movie::getName, dto.getName()));
        if (count > 0) {
            throw new MovieException(ErrorCode.MOVIE_NAME_DUPLICATE);
        }

        Movie movie = new Movie();
        BeanUtils.copyProperties(dto, movie);
        save(movie);

        log.info("影片新增成功, id: {}, name: {}", movie.getId(), movie.getName());
        return toVO(movie);
    }

    @Override
    public MovieVO updateMovie(Long id, MovieUpdateDTO dto) {
        Movie movie = getById(id);
        if (movie == null) {
            throw new MovieException(ErrorCode.MOVIE_NOT_FOUND);
        }

        if (StringUtils.hasText(dto.getName()) && !dto.getName().equals(movie.getName())) {
            long count = count(new LambdaQueryWrapper<Movie>().eq(Movie::getName, dto.getName()));
            if (count > 0) {
                throw new MovieException(ErrorCode.MOVIE_NAME_DUPLICATE);
            }
        }

        List<String> updatedFields = new ArrayList<>();
        if (StringUtils.hasText(dto.getName())) { movie.setName(dto.getName()); updatedFields.add("name"); }
        if (StringUtils.hasText(dto.getGenre())) { movie.setGenre(dto.getGenre()); updatedFields.add("genre"); }
        if (dto.getDuration() != null) { movie.setDuration(dto.getDuration()); updatedFields.add("duration"); }
        if (dto.getRating() != null) { movie.setRating(dto.getRating()); updatedFields.add("rating"); }
        if (dto.getPoster() != null) { movie.setPoster(dto.getPoster()); updatedFields.add("poster"); }
        if (dto.getDescription() != null) { movie.setDescription(dto.getDescription()); updatedFields.add("description"); }
        if (dto.getCast() != null) { movie.setCast(dto.getCast()); updatedFields.add("cast"); }
        if (dto.getReleaseDate() != null) { movie.setReleaseDate(dto.getReleaseDate()); updatedFields.add("releaseDate"); }

        updateById(movie);

        log.info("影片编辑成功, id: {}, updatedFields: {}", id, updatedFields);
        return toVO(movie);
    }

    @Override
    public MovieVO updateMovieStatus(Long id, MovieStatusDTO dto) {
        Movie movie = getById(id);
        if (movie == null) {
            throw new MovieException(ErrorCode.MOVIE_NOT_FOUND);
        }

        if (dto.getStatus() == MovieStatus.OFFLINE && movie.getStatus() == MovieStatus.NOW_SHOWING) {
            // TODO: 接入 showtime 表后查询 count WHERE movie_id = id AND status = ON_SALE
            log.warn("影片下架操作，暂未校验在售场次, movieId: {}", id);
        }

        movie.setStatus(dto.getStatus());
        updateById(movie);

        log.info("影片状态变更, id: {}, newStatus: {}", id, dto.getStatus());
        return toVO(movie);
    }

    private LambdaQueryWrapper<Movie> buildQueryWrapper(String keyword, String status) {
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Movie::getName, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Movie::getStatus, MovieStatus.valueOf(status));
        }
        return wrapper;
    }

    private MovieVO toVO(Movie movie) {
        MovieVO vo = new MovieVO();
        BeanUtils.copyProperties(movie, vo);
        vo.setStatus(movie.getStatus() != null ? movie.getStatus().getCode() : null);
        vo.setStatusDesc(movie.getStatus() != null ? movie.getStatus().getDesc() : null);
        vo.setShowtimeCount(0);
        return vo;
    }
}
