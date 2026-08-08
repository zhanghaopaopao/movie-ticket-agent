package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.dto.MovieCreateDTO;
import com.szml.movieticket.dto.MovieStatusDTO;
import com.szml.movieticket.dto.MovieUpdateDTO;
import com.szml.movieticket.entity.Cinema;
import com.szml.movieticket.entity.Hall;
import com.szml.movieticket.entity.Movie;
import com.szml.movieticket.entity.Showtime;
import com.szml.movieticket.entity.UserMovieWishlist;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.util.AmountUtil;
import com.szml.movieticket.enums.MovieStatus;
import com.szml.movieticket.enums.ShowtimeStatus;
import com.szml.movieticket.exception.MovieException;
import com.szml.movieticket.mapper.CinemaMapper;
import com.szml.movieticket.mapper.HallMapper;
import com.szml.movieticket.mapper.MovieMapper;
import com.szml.movieticket.mapper.ShowtimeMapper;
import com.szml.movieticket.mapper.UserMovieWishlistMapper;
import com.szml.movieticket.service.MovieService;
import com.szml.movieticket.vo.MovieOptionVO;
import com.szml.movieticket.vo.MoviePageVO;
import com.szml.movieticket.vo.MovieVO;
import com.szml.movieticket.vo.ShowtimeSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final UserMovieWishlistMapper wishlistMapper;

    @Override
    public MoviePageVO pageMovies(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<Movie> wrapper = buildQueryWrapper(keyword, status);
        wrapper.orderByDesc(Movie::getReleaseDate);

        Page<Movie> pageResult = page(new Page<>(page, size), wrapper);
        List<MovieVO> records = buildMovieVOList(pageResult.getRecords());

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
    public void createMovie(MovieCreateDTO dto) {
        long count = count(new LambdaQueryWrapper<Movie>().eq(Movie::getName, dto.getName()));
        if (count > 0) {
            throw new MovieException(ErrorCode.MOVIE_NAME_DUPLICATE);
        }

        // 边界校验：上映状态与上映日期的逻辑关系
        validateMovieStatusAndDate(dto.getStatus(), dto.getReleaseDate(), true);

        Movie movie = new Movie();
        BeanUtils.copyProperties(dto, movie);
        save(movie);

        log.info("影片新增成功, id: {}, name: {}", movie.getId(), movie.getName());
    }

    @Override
    public void updateMovie(Long id, MovieUpdateDTO dto) {
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
        if (dto.getDuration() != null && !dto.getDuration().equals(movie.getDuration())) {
            long showtimeCount = showtimeMapper.selectCount(
                    new LambdaQueryWrapper<Showtime>().eq(Showtime::getMovieId, id));
            if (showtimeCount > 0) {
                throw new MovieException(ErrorCode.MOVIE_DURATION_IMMUTABLE);
            }
            movie.setDuration(dto.getDuration());
            updatedFields.add("duration");
        }
        if (dto.getRating() != null) { movie.setRating(dto.getRating()); updatedFields.add("rating"); }
        if (dto.getPoster() != null) { movie.setPoster(dto.getPoster()); updatedFields.add("poster"); }
        if (dto.getDescription() != null) { movie.setDescription(dto.getDescription()); updatedFields.add("description"); }
        if (dto.getCast() != null) { movie.setCast(dto.getCast()); updatedFields.add("cast"); }
        if (dto.getReleaseDate() != null && !dto.getReleaseDate().equals(movie.getReleaseDate())) {
            // 有关联场次时，releaseDate 不能晚于最早在售场次日期
            List<Showtime> showtimes = showtimeMapper.selectList(
                    new LambdaQueryWrapper<Showtime>()
                            .eq(Showtime::getMovieId, id)
                            .orderByAsc(Showtime::getStartAt));
            if (!showtimes.isEmpty()) {
                LocalDate earliestShowtimeDate = showtimes.getFirst().getStartAt().toLocalDate();
                if (dto.getReleaseDate().isAfter(earliestShowtimeDate)) {
                    throw new MovieException(ErrorCode.MOVIE_RELEASE_DATE_AFTER_SHOWTIME);
                }
            }
            movie.setReleaseDate(dto.getReleaseDate());
            updatedFields.add("releaseDate");
        }
        if (dto.getStatus() != null) {
            // 下架保护：只要有在售场次就不允许下架
            if (dto.getStatus() == MovieStatus.OFFLINE) {
                long activeCount = showtimeMapper.selectCount(
                        new LambdaQueryWrapper<Showtime>()
                                .eq(Showtime::getMovieId, id)
                                .eq(Showtime::getStatus, ShowtimeStatus.ON_SALE));
                if (activeCount > 0) {
                    throw new MovieException(ErrorCode.MOVIE_HAS_ACTIVE_SHOWTIMES);
                }
            }
            // 已下架影片重新上架时，根据上映日期自动判定状态
            if (movie.getStatus() == MovieStatus.OFFLINE && dto.getStatus() != MovieStatus.OFFLINE) {
                movie.setStatus(movie.getReleaseDate().isAfter(LocalDate.now())
                        ? MovieStatus.COMING_SOON : MovieStatus.NOW_SHOWING);
            } else {
                movie.setStatus(dto.getStatus());
            }
            updatedFields.add("status");
        }

        // 编辑后校验最终状态与上映日期的关系
        validateMovieStatusAndDate(movie.getStatus(), movie.getReleaseDate(), false);

        updateById(movie);

        log.info("影片编辑成功, id: {}, updatedFields: {}", id, updatedFields);
    }

    @Override
    public void updateMovieStatus(Long id, MovieStatusDTO dto) {
        Movie movie = getById(id);
        if (movie == null) {
            throw new MovieException(ErrorCode.MOVIE_NOT_FOUND);
        }

        if (dto.getStatus() == MovieStatus.OFFLINE) {
            long activeCount = showtimeMapper.selectCount(
                    new LambdaQueryWrapper<Showtime>()
                            .eq(Showtime::getMovieId, id)
                            .eq(Showtime::getStatus, ShowtimeStatus.ON_SALE));
            if (activeCount > 0) {
                throw new MovieException(ErrorCode.MOVIE_HAS_ACTIVE_SHOWTIMES);
            }
        }

        // 已下架影片重新上架时，系统根据上映日期自动判定状态
        if (movie.getStatus() == MovieStatus.OFFLINE && dto.getStatus() != MovieStatus.OFFLINE) {
            movie.setStatus(movie.getReleaseDate().isAfter(LocalDate.now())
                    ? MovieStatus.COMING_SOON : MovieStatus.NOW_SHOWING);
        } else {
            movie.setStatus(dto.getStatus());
        }
        updateById(movie);

        log.info("影片状态变更, id: {}, newStatus: {}", id, dto.getStatus());
    }

    @Override
    public void deleteMovie(Long id) {
        Movie movie = getById(id);
        if (movie == null) {
            throw new MovieException(ErrorCode.MOVIE_NOT_FOUND);
        }

        long showtimeCount = showtimeMapper.selectCount(
                new LambdaQueryWrapper<Showtime>().eq(Showtime::getMovieId, id));
        if (showtimeCount > 0) {
            throw new MovieException(ErrorCode.MOVIE_HAS_ASSOCIATED_SHOWTIMES);
        }

        removeById(id);
        log.info("影片删除成功, id: {}, name: {}", id, movie.getName());
    }

    @Override
    public List<MovieOptionVO> listMovieOptions() {
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<Movie>()
                .ne(Movie::getStatus, MovieStatus.OFFLINE)
                .select(Movie::getId, Movie::getName)
                .orderByAsc(Movie::getCreateTime);
        return list(wrapper).stream()
                .map(m -> { MovieOptionVO vo = new MovieOptionVO(); vo.setId(m.getId()); vo.setName(m.getName()); return vo; })
                .collect(Collectors.toList());
    }

    @Override
    public MoviePageVO listMoviesForUser(Long userId, int page, int size, String status, String genre, String keyword,
                                         String sortBy, String sortOrder) {
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
        applyUserMovieSort(wrapper, sortBy, sortOrder);

        Page<Movie> pageResult = page(new Page<>(page, size), wrapper);
        List<MovieVO> records = buildMovieVOList(pageResult.getRecords());
        markWanted(userId, records);

        MoviePageVO pageVO = new MoviePageVO();
        pageVO.setTotal(pageResult.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setRecords(records);
        return pageVO;
    }

    @Override
    public MoviePageVO listMoviesWithShowtimes(Long userId, int page, int size, String genre, String keyword,
                                               String sortBy, String sortOrder, LocalDate date) {
        int fetchSize = Math.max(size * 2, 50);
        LambdaQueryWrapper<Movie> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(genre)) {
            wrapper.like(Movie::getGenre, genre);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Movie::getName, keyword);
        }
        applyUserMovieSort(wrapper, sortBy, sortOrder);

        Page<Movie> pageResult = page(new Page<>(1, fetchSize), wrapper);
        List<Movie> movies = pageResult.getRecords();
        if (movies.isEmpty()) {
            MoviePageVO empty = new MoviePageVO();
            empty.setTotal(0); empty.setPage(page); empty.setSize(size); empty.setRecords(List.of());
            return empty;
        }

        // 只查询未来有在售场次，按日期过滤
        List<Long> movieIds = movies.stream().map(Movie::getId).toList();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromTime = date != null
                ? date.atStartOfDay()
                : now;
        LocalDateTime toTime = date != null
                ? date.plusDays(1).atStartOfDay()
                : null;
        LambdaQueryWrapper<Showtime> showtimeWrapper = new LambdaQueryWrapper<Showtime>()
                .in(Showtime::getMovieId, movieIds)
                .eq(Showtime::getStatus, ShowtimeStatus.ON_SALE)
                .gt(Showtime::getStartAt, fromTime);
        if (toTime != null) {
            showtimeWrapper.lt(Showtime::getStartAt, toTime);
        }
        List<Showtime> futureShowtimes = showtimeMapper.selectList(
                showtimeWrapper.orderByAsc(Showtime::getStartAt));

        // 一次性查影厅和影院，避免 N+1
        Set<Long> hallIds = futureShowtimes.stream().map(Showtime::getHallId).collect(Collectors.toSet());
        Map<Long, Hall> hallMap = new HashMap<>();
        Map<Long, String> cinemaNameMap = new HashMap<>();
        if (!hallIds.isEmpty()) {
            for (Hall h : hallMapper.selectBatchIds(hallIds)) {
                hallMap.put(h.getId(), h);
                if (!cinemaNameMap.containsKey(h.getCinemaId())) {
                    Cinema cinema = cinemaMapper.selectById(h.getCinemaId());
                    if (cinema != null) {
                        cinemaNameMap.put(h.getCinemaId(), cinema.getName());
                    }
                }
            }
        }

        // 按 movieId 分组场次，每部影片最多 5 场
        Map<Long, List<ShowtimeSummaryVO>> showtimeMap = new HashMap<>();
        for (Showtime st : futureShowtimes) {
            List<ShowtimeSummaryVO> list = showtimeMap.computeIfAbsent(
                    st.getMovieId(), k -> new ArrayList<>());
            if (list.size() >= 5) continue;
            Hall hall = hallMap.get(st.getHallId());
            ShowtimeSummaryVO summary = new ShowtimeSummaryVO();
            summary.setShowtimeId(st.getId());
            summary.setHallName(hall != null ? hall.getName() : null);
            if (hall != null) {
                summary.setCinemaName(cinemaNameMap.get(hall.getCinemaId()));
            }
            summary.setStartAt(st.getStartAt());
            summary.setEndAt(st.getEndAt());
            summary.setPrice(st.getBasePrice() != null
                    ? String.format("%.2f", AmountUtil.yuan(st.getBasePrice()))
                    : null);
            summary.setRemainingSeats(null); // Agent 不需要精确余量
            list.add(summary);
        }

        Set<Long> moviesWithFutureShowtimes = showtimeMap.keySet();

        List<MovieVO> allRecords = buildMovieVOList(movies);
        List<MovieVO> withShowtimes = allRecords.stream()
                .filter(m -> moviesWithFutureShowtimes.contains(m.getId()))
                .peek(m -> m.setUpcomingShowtimes(showtimeMap.get(m.getId())))
                .collect(Collectors.toList());

        int total = withShowtimes.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<MovieVO> pageRecords = fromIndex < total
                ? withShowtimes.subList(fromIndex, toIndex)
                : List.of();
        markWanted(userId, pageRecords);

        MoviePageVO pageVO = new MoviePageVO();
        pageVO.setTotal(total);
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setRecords(pageRecords);
        return pageVO;
    }

    void applyUserMovieSort(LambdaQueryWrapper<Movie> wrapper, String sortBy, String sortOrder) {
        boolean ascending = switch (sortOrder) {
            case "asc" -> true;
            case "desc" -> false;
            default -> throw new MovieException(ErrorCode.PARAM_ERROR);
        };

        switch (sortBy) {
            case "createTime" -> wrapper.orderBy(true, ascending, Movie::getCreateTime);
            case "releaseDate" -> wrapper.orderBy(true, ascending, Movie::getReleaseDate);
            case "rating" -> wrapper.orderBy(true, ascending, Movie::getRating);
            default -> throw new MovieException(ErrorCode.PARAM_ERROR);
        }
    }

    @Override
    public MovieVO getMovieDetailForUser(Long userId, Long id) {
        Movie movie = getById(id);
        if (movie == null) {
            throw new MovieException(ErrorCode.MOVIE_NOT_FOUND);
        }
        MovieVO vo = toVO(movie);
        vo.setWanted(wishlistMapper.selectCount(new LambdaQueryWrapper<UserMovieWishlist>()
                .eq(UserMovieWishlist::getUserId, userId)
                .eq(UserMovieWishlist::getMovieId, id)) > 0);
        return vo;
    }

    private void markWanted(Long userId, List<MovieVO> movies) {
        if (movies.isEmpty()) return;
        List<Long> movieIds = movies.stream().map(MovieVO::getId).toList();
        Set<Long> wantedMovieIds = wishlistMapper.selectList(
                new LambdaQueryWrapper<UserMovieWishlist>()
                        .eq(UserMovieWishlist::getUserId, userId)
                        .in(UserMovieWishlist::getMovieId, movieIds))
                .stream()
                .map(UserMovieWishlist::getMovieId)
                .collect(Collectors.toSet());
        movies.forEach(movie -> movie.setWanted(wantedMovieIds.contains(movie.getId())));
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

    private List<MovieVO> buildMovieVOList(List<Movie> movies) {
        if (movies.isEmpty()) {
            return List.of();
        }

        // 1. 收集所有影片ID，一次查所有在售场次
        List<Long> movieIds = movies.stream().map(Movie::getId).toList();
        List<Showtime> allShowtimes = showtimeMapper.selectList(
                new LambdaQueryWrapper<Showtime>()
                        .in(Showtime::getMovieId, movieIds)
                        .eq(Showtime::getStatus, ShowtimeStatus.ON_SALE));

        // 2. 一次查所有相关影厅
        Map<Long, Hall> hallMap = new HashMap<>();
        if (!allShowtimes.isEmpty()) {
            Set<Long> hallIds = allShowtimes.stream().map(Showtime::getHallId).collect(Collectors.toSet());
            for (Hall h : hallMapper.selectBatchIds(hallIds)) {
                hallMap.put(h.getId(), h);
            }
        }

        // 3. 内存计算每部影片的场次数和覆盖影院数
        Map<Long, Integer> showtimeCountMap = new HashMap<>();
        Map<Long, Set<Long>> cinemaCountMap = new HashMap<>();
        for (Showtime st : allShowtimes) {
            showtimeCountMap.merge(st.getMovieId(), 1, Integer::sum);
            Hall hall = hallMap.get(st.getHallId());
            if (hall != null) {
                cinemaCountMap.computeIfAbsent(st.getMovieId(), k -> new HashSet<>()).add(hall.getCinemaId());
            }
        }

        // 4. 组装
        return movies.stream().map(m -> {
            MovieVO vo = new MovieVO();
            BeanUtils.copyProperties(m, vo);
            vo.setStatus(m.getStatus() != null ? m.getStatus().getCode() : null);
            vo.setStatusDesc(m.getStatus() != null ? m.getStatus().getDesc() : null);
            vo.setShowtimeCount(showtimeCountMap.getOrDefault(m.getId(), 0));
            vo.setCinemaCount(cinemaCountMap.getOrDefault(m.getId(), Set.of()).size());
            return vo;
        }).collect(Collectors.toList());
    }

    private MovieVO toVO(Movie movie) {
        MovieVO vo = new MovieVO();
        BeanUtils.copyProperties(movie, vo);
        vo.setStatus(movie.getStatus() != null ? movie.getStatus().getCode() : null);
        vo.setStatusDesc(movie.getStatus() != null ? movie.getStatus().getDesc() : null);

        // 关联在售场次和覆盖影院数
        List<Showtime> activeShowtimes = showtimeMapper.selectList(
                new LambdaQueryWrapper<Showtime>()
                        .eq(Showtime::getMovieId, movie.getId())
                        .eq(Showtime::getStatus, ShowtimeStatus.ON_SALE));
        vo.setShowtimeCount(activeShowtimes.size());

        Set<Long> cinemaIds = new HashSet<>();
        for (Showtime st : activeShowtimes) {
            Hall hall = hallMapper.selectById(st.getHallId());
            if (hall != null) cinemaIds.add(hall.getCinemaId());
        }
        vo.setCinemaCount(cinemaIds.size());
        return vo;
    }

    /**
     * 校验上映状态与上映日期之间的边界约束。
     */
    private void validateMovieStatusAndDate(MovieStatus status, LocalDate releaseDate, boolean isCreate) {
        if (status == null || releaseDate == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        switch (status) {
            case NOW_SHOWING:
                if (releaseDate.isAfter(today)) {
                    throw new MovieException(ErrorCode.MOVIE_RELEASE_DATE_PAST);
                }
                break;
            case COMING_SOON:
                if (!releaseDate.isAfter(today)) {
                    throw new MovieException(ErrorCode.MOVIE_RELEASE_DATE_FUTURE);
                }
                break;
            case OFFLINE:
                if (isCreate) {
                    throw new MovieException(ErrorCode.MOVIE_STATUS_INVALID);
                }
                break;
        }
    }
}
