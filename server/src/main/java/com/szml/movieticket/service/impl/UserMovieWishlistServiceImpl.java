package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szml.movieticket.entity.Movie;
import com.szml.movieticket.entity.UserMovieWishlist;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.BusinessException;
import com.szml.movieticket.exception.MovieException;
import com.szml.movieticket.mapper.MovieMapper;
import com.szml.movieticket.mapper.UserMovieWishlistMapper;
import com.szml.movieticket.service.UserMovieWishlistService;
import com.szml.movieticket.vo.MoviePageVO;
import com.szml.movieticket.vo.MovieVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMovieWishlistServiceImpl implements UserMovieWishlistService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserMovieWishlistMapper wishlistMapper;
    private final MovieMapper movieMapper;

    @Override
    public MoviePageVO list(Long userId, int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }

        Page<UserMovieWishlist> wishlistPage = wishlistMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<UserMovieWishlist>()
                        .eq(UserMovieWishlist::getUserId, userId)
                        .orderByDesc(UserMovieWishlist::getCreateTime)
                        .orderByDesc(UserMovieWishlist::getId));

        List<Long> movieIds = wishlistPage.getRecords().stream()
                .map(UserMovieWishlist::getMovieId)
                .toList();
        Map<Long, Movie> moviesById = movieIds.isEmpty()
                ? Collections.emptyMap()
                : movieMapper.selectBatchIds(movieIds).stream()
                        .collect(Collectors.toMap(Movie::getId, Function.identity()));

        List<MovieVO> records = movieIds.stream()
                .map(moviesById::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toVO)
                .toList();

        MoviePageVO result = new MoviePageVO();
        result.setPage(page);
        result.setSize(size);
        result.setTotal(wishlistPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public void add(Long userId, Long movieId) {
        if (movieMapper.selectById(movieId) == null) {
            throw new MovieException(ErrorCode.MOVIE_NOT_FOUND);
        }
        wishlistMapper.insertIgnore(userId, movieId);
        log.debug("加入想看, userId: {}, movieId: {}", userId, movieId);
    }

    @Override
    public void remove(Long userId, Long movieId) {
        wishlistMapper.delete(new LambdaQueryWrapper<UserMovieWishlist>()
                .eq(UserMovieWishlist::getUserId, userId)
                .eq(UserMovieWishlist::getMovieId, movieId));
        log.debug("取消想看, userId: {}, movieId: {}", userId, movieId);
    }

    private MovieVO toVO(Movie movie) {
        MovieVO vo = new MovieVO();
        BeanUtils.copyProperties(movie, vo);
        vo.setStatus(movie.getStatus() != null ? movie.getStatus().getCode() : null);
        vo.setStatusDesc(movie.getStatus() != null ? movie.getStatus().getDesc() : null);
        vo.setWanted(true);
        return vo;
    }
}
