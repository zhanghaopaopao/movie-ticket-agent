package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szml.movieticket.entity.Movie;
import com.szml.movieticket.entity.UserMovieWatched;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.BusinessException;
import com.szml.movieticket.exception.MovieException;
import com.szml.movieticket.mapper.MovieMapper;
import com.szml.movieticket.mapper.UserMovieWatchedMapper;
import com.szml.movieticket.service.UserMovieWatchedService;
import com.szml.movieticket.vo.MoviePageVO;
import com.szml.movieticket.vo.MovieVO;
import org.springframework.beans.BeanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 用户已看过影片服务实现。 */
@Service
@RequiredArgsConstructor
public class UserMovieWatchedServiceImpl implements UserMovieWatchedService {

    private final UserMovieWatchedMapper watchedMapper;
    private final MovieMapper movieMapper;

    @Override
    public MoviePageVO list(Long userId, int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Page<UserMovieWatched> watchedPage = watchedMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<UserMovieWatched>()
                        .eq(UserMovieWatched::getUserId, userId)
                        .orderByDesc(UserMovieWatched::getCreateTime)
                        .orderByDesc(UserMovieWatched::getId));
        List<Long> ids = watchedPage.getRecords().stream().map(UserMovieWatched::getMovieId).toList();
        Map<Long, Movie> movies = ids.isEmpty() ? Collections.emptyMap() : movieMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));
        List<MovieVO> records = ids.stream().map(movies::get).filter(java.util.Objects::nonNull).map(movie -> {
            MovieVO vo = new MovieVO();
            BeanUtils.copyProperties(movie, vo);
            vo.setStatus(movie.getStatus() == null ? null : movie.getStatus().getCode());
            vo.setStatusDesc(movie.getStatus() == null ? null : movie.getStatus().getDesc());
            vo.setWanted(false);
            return vo;
        }).toList();
        MoviePageVO result = new MoviePageVO();
        result.setPage(page);
        result.setSize(size);
        result.setTotal(watchedPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public boolean isWatched(Long userId, Long movieId) {
        return watchedMapper.selectCount(new LambdaQueryWrapper<UserMovieWatched>()
                .eq(UserMovieWatched::getUserId, userId)
                .eq(UserMovieWatched::getMovieId, movieId)) > 0;
    }

    @Override
    public void add(Long userId, Long movieId) {
        if (movieMapper.selectById(movieId) == null) throw new MovieException(ErrorCode.MOVIE_NOT_FOUND);
        watchedMapper.insertIgnore(userId, movieId);
    }

    @Override
    public void remove(Long userId, Long movieId) {
        watchedMapper.delete(new LambdaQueryWrapper<UserMovieWatched>()
                .eq(UserMovieWatched::getUserId, userId)
                .eq(UserMovieWatched::getMovieId, movieId));
    }
}
