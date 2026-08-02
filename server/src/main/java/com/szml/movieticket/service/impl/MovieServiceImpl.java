package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.dto.MovieCreateDTO;
import com.szml.movieticket.dto.MovieStatusDTO;
import com.szml.movieticket.dto.MovieUpdateDTO;
import com.szml.movieticket.entity.Hall;
import com.szml.movieticket.entity.Movie;
import com.szml.movieticket.entity.Showtime;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.enums.MovieStatus;
import com.szml.movieticket.enums.ShowtimeStatus;
import com.szml.movieticket.exception.MovieException;
import com.szml.movieticket.mapper.CinemaMapper;
import com.szml.movieticket.mapper.HallMapper;
import com.szml.movieticket.mapper.MovieMapper;
import com.szml.movieticket.mapper.ShowtimeMapper;
import com.szml.movieticket.service.MovieService;
import com.szml.movieticket.vo.MoviePageVO;
import com.szml.movieticket.vo.MovieVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 影片服务实现类。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieServiceImpl extends ServiceImpl<MovieMapper, Movie> implements MovieService {

    private final ShowtimeMapper showtimeMapper;
    private final CinemaMapper cinemaMapper;
    private final HallMapper hallMapper;

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
            long activeCount = showtimeMapper.selectCount(
                    new LambdaQueryWrapper<Showtime>()
                            .eq(Showtime::getMovieId, id)
                            .eq(Showtime::getStatus, ShowtimeStatus.ON_SALE));
            if (activeCount > 0) {
                throw new MovieException(ErrorCode.MOVIE_HAS_ACTIVE_SHOWTIMES);
            }
        }

        movie.setStatus(dto.getStatus());
        updateById(movie);

        log.info("影片状态变更, id: {}, newStatus: {}", id, dto.getStatus());
        return toVO(movie);
    }

    @Override
    public MoviePageVO listMoviesForUser(int page, int size, String status, String genre, String keyword) {
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Movie::getStatus, MovieStatus.valueOf(status));
        }
        if (StringUtils.hasText(genre)) {
            wrapper.like(Movie::getGenre, genre);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Movie::getName, keyword);
        }
        wrapper.orderByDesc(Movie::getCreateTime);

        Page<Movie> pageResult = page(new Page<>(page, size), wrapper);
        List<MovieVO> records = pageResult.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());

        MoviePageVO pageVO = new MoviePageVO();
        pageVO.setTotal(pageResult.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setRecords(records);
        return pageVO;
    }

    @Override
    public MovieVO getMovieDetailForUser(Long id) {
        Movie movie = getById(id);
        if (movie == null) {
            throw new MovieException(ErrorCode.MOVIE_NOT_FOUND);
        }
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

        // 当日在售场次和覆盖影院数
        LocalDate today = LocalDate.now();
        List<Showtime> todayShortimes = showtimeMapper.selectList(
                new LambdaQueryWrapper<Showtime>()
                        .eq(Showtime::getMovieId, movie.getId())
                        .eq(Showtime::getStatus, ShowtimeStatus.ON_SALE)
                        .ge(Showtime::getStartAt, today.atStartOfDay())
                        .lt(Showtime::getStartAt, today.plusDays(1).atStartOfDay()));
        vo.setShowtimeCount(todayShortimes.size());

        Set<Long> cinemaIds = new HashSet<>();
        for (Showtime st : todayShortimes) {
            Hall hall = hallMapper.selectById(st.getHallId());
            if (hall != null) cinemaIds.add(hall.getCinemaId());
        }
        vo.setCinemaCount(cinemaIds.size());
        return vo;
    }
}
