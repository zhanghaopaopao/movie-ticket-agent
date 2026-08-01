package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.dto.ShowtimeCreateDTO;
import com.szml.movieticket.dto.ShowtimeSeatStatusDTO;
import com.szml.movieticket.dto.ShowtimeStatusDTO;
import com.szml.movieticket.dto.ShowtimeUpdateDTO;
import com.szml.movieticket.entity.Cinema;
import com.szml.movieticket.entity.Hall;
import com.szml.movieticket.entity.Movie;
import com.szml.movieticket.entity.Showtime;
import com.szml.movieticket.entity.ShowtimeSeat;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.enums.ShowtimeStatus;
import com.szml.movieticket.exception.ShowtimeException;
import com.szml.movieticket.mapper.CinemaMapper;
import com.szml.movieticket.mapper.HallMapper;
import com.szml.movieticket.mapper.MovieMapper;
import com.szml.movieticket.mapper.ShowtimeMapper;
import com.szml.movieticket.mapper.ShowtimeSeatMapper;
import com.szml.movieticket.service.ShowtimeService;
import com.szml.movieticket.vo.ShowtimePageVO;
import com.szml.movieticket.vo.ShowtimeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 场次服务实现类。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeServiceImpl extends ServiceImpl<ShowtimeMapper, Showtime> implements ShowtimeService {

    private static final int CLEANING_MINUTES = 10;

    private final MovieMapper movieMapper;
    private final HallMapper hallMapper;
    private final CinemaMapper cinemaMapper;
    private final ShowtimeSeatMapper showtimeSeatMapper;

    @Override
    public ShowtimePageVO pageShowtimes(int page, int size, Long movieId, Long cinemaId, String date, String status) {
        LambdaQueryWrapper<Showtime> wrapper = new LambdaQueryWrapper<>();
        if (movieId != null) {
            wrapper.eq(Showtime::getMovieId, movieId);
        }
        if (cinemaId != null) {
            // cinemaId 在 hall 表，需先查出该影院的所有影厅ID
            List<Hall> halls = hallMapper.selectList(new LambdaQueryWrapper<Hall>().eq(Hall::getCinemaId, cinemaId));
            List<Long> hallIds = halls.stream().map(Hall::getId).collect(Collectors.toList());
            if (hallIds.isEmpty()) {
                ShowtimePageVO empty = new ShowtimePageVO();
                empty.setTotal(0); empty.setPage(page); empty.setSize(size);
                empty.setRecords(new ArrayList<>());
                return empty;
            }
            wrapper.in(Showtime::getHallId, hallIds);
        }
        if (StringUtils.hasText(date)) {
            LocalDateTime start = LocalDateTime.parse(date + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(date + "T23:59:59");
            wrapper.between(Showtime::getStartAt, start, end);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Showtime::getStatus, ShowtimeStatus.fromCode(Integer.parseInt(status)));
        }
        wrapper.orderByAsc(Showtime::getStartAt);

        Page<Showtime> pageResult = page(new Page<>(page, size), wrapper);
        List<ShowtimeVO> records = pageResult.getRecords().stream().map(this::toVO).collect(Collectors.toList());

        ShowtimePageVO pageVO = new ShowtimePageVO();
        pageVO.setTotal(pageResult.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setRecords(records);
        return pageVO;
    }

    @Override
    public ShowtimeVO createShowtime(ShowtimeCreateDTO dto) {
        Movie movie = movieMapper.selectById(dto.getMovieId());
        if (movie == null) {
            throw new ShowtimeException(ErrorCode.MOVIE_NOT_FOUND);
        }
        Hall hall = hallMapper.selectById(dto.getHallId());
        if (hall == null) {
            throw new ShowtimeException(ErrorCode.HALL_NOT_FOUND);
        }

        // 计算散场时间
        LocalDateTime startAt = dto.getStartAt();
        LocalDateTime endAt = startAt.plusMinutes(movie.getDuration() + CLEANING_MINUTES);

        // 时间冲突校验：同一影厅，新场次不能与已有场次重叠且间隔 ≥ 20min
        checkTimeConflict(dto.getHallId(), null, startAt, endAt);

        Showtime showtime = new Showtime();
        BeanUtils.copyProperties(dto, showtime);
        showtime.setEndAt(endAt);
        showtime.setLanguage(StringUtils.hasText(dto.getLanguage()) ? dto.getLanguage() : "国语2D");
        showtime.setStatus(ShowtimeStatus.ON_SALE);
        save(showtime);

        // TODO: 从 seat WHERE hall_id = ? 全量复制到 showtime_seat
        log.info("场次新增成功, id: {}, movieId: {}, hallId: {}, startAt: {}", showtime.getId(), dto.getMovieId(), dto.getHallId(), startAt);

        return toVO(showtime);
    }

    @Override
    public ShowtimeVO updateShowtime(Long id, ShowtimeUpdateDTO dto) {
        Showtime showtime = getById(id);
        if (showtime == null) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_NOT_FOUND);
        }

        List<String> updatedFields = new ArrayList<>();

        if (dto.getStartAt() != null) {
            // TODO: 校验是否已有锁座或订单
            log.warn("修改场次时间，暂未校验锁座订单, showtimeId: {}", id);

            // 重新计算 endAt
            Movie movie = movieMapper.selectById(showtime.getMovieId());
            LocalDateTime endAt = dto.getStartAt().plusMinutes(movie != null ? movie.getDuration() + CLEANING_MINUTES : 0);
            checkTimeConflict(showtime.getHallId(), id, dto.getStartAt(), endAt);
            showtime.setStartAt(dto.getStartAt());
            showtime.setEndAt(endAt);
            updatedFields.add("startAt");
            updatedFields.add("endAt");
        }
        if (dto.getBasePrice() != null) {
            showtime.setBasePrice(dto.getBasePrice());
            updatedFields.add("basePrice");
        }
        if (dto.getLanguage() != null) {
            showtime.setLanguage(dto.getLanguage());
            updatedFields.add("language");
        }

        updateById(showtime);

        log.info("场次编辑成功, id: {}, updatedFields: {}", id, updatedFields);
        return toVO(showtime);
    }

    @Override
    public ShowtimeVO updateShowtimeStatus(Long id, ShowtimeStatusDTO dto) {
        Showtime showtime = getById(id);
        if (showtime == null) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_NOT_FOUND);
        }

        if (dto.getStatus() == ShowtimeStatus.CANCELLED) {
            // TODO: 校验是否存在已锁定座位
            log.warn("场次取消操作，暂未校验已锁定座位, showtimeId: {}", id);
        }

        showtime.setStatus(dto.getStatus());
        updateById(showtime);

        log.info("场次状态变更, id: {}, newStatus: {}", id, dto.getStatus());
        return toVO(showtime);
    }

    @Override
    public Map<String, Object> updateSeatStatus(Long showtimeId, ShowtimeSeatStatusDTO dto) {
        List<Long> updatedSeatIds = new ArrayList<>();
        List<Long> skippedSeatIds = new ArrayList<>();

        List<ShowtimeSeat> seats = showtimeSeatMapper.selectList(
                new LambdaQueryWrapper<ShowtimeSeat>()
                        .eq(ShowtimeSeat::getShowtimeId, showtimeId)
                        .in(ShowtimeSeat::getId, dto.getSeatIds()));

        for (ShowtimeSeat seat : seats) {
            if ("UNAVAILABLE".equals(dto.getStatus())) {
                // 已售(2)或已锁(1)的不可修改
                if (seat.getStatus() == 2 || seat.getStatus() == 1) {
                    skippedSeatIds.add(seat.getId());
                    continue;
                }
                seat.setStatus(3); // UNAVAILABLE
                updatedSeatIds.add(seat.getId());
            } else if ("AVAILABLE".equals(dto.getStatus())) {
                // 仅 UNAVAILABLE(3) 的可恢复
                if (seat.getStatus() != 3) {
                    skippedSeatIds.add(seat.getId());
                    continue;
                }
                seat.setStatus(0); // AVAILABLE
                updatedSeatIds.add(seat.getId());
            }
        }

        for (ShowtimeSeat seat : seats) {
            if (updatedSeatIds.contains(seat.getId())) {
                showtimeSeatMapper.updateById(seat);
            }
        }

        log.info("座位状态批量更新, showtimeId: {}, targetStatus: {}, updated: {}, skipped: {}",
                showtimeId, dto.getStatus(), updatedSeatIds.size(), skippedSeatIds.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updatedSeatIds", updatedSeatIds);
        result.put("skippedSeatIds", skippedSeatIds);
        if (!skippedSeatIds.isEmpty()) {
            result.put("skippedReason", "座位已被售出或锁定，不可修改状态");
        }
        return result;
    }

    /**
     * 校验同一影厅场次时间冲突：不可重叠，间隔 ≥ 20 分钟。
     */
    private void checkTimeConflict(Long hallId, Long excludeShowtimeId, LocalDateTime startAt, LocalDateTime endAt) {
        LocalDateTime conflictStart = startAt.minusMinutes(20);
        LocalDateTime conflictEnd = endAt.plusMinutes(20);

        LambdaQueryWrapper<Showtime> wrapper = new LambdaQueryWrapper<Showtime>()
                .eq(Showtime::getHallId, hallId)
                .ne(Showtime::getStatus, ShowtimeStatus.CANCELLED)
                .ge(Showtime::getEndAt, conflictStart)
                .le(Showtime::getStartAt, conflictEnd);
        if (excludeShowtimeId != null) {
            wrapper.ne(Showtime::getId, excludeShowtimeId);
        }

        long count = count(wrapper);
        if (count > 0) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_TIME_CONFLICT);
        }
    }

    private ShowtimeVO toVO(Showtime showtime) {
        ShowtimeVO vo = new ShowtimeVO();
        BeanUtils.copyProperties(showtime, vo);

        // 嵌套对象
        Movie movie = movieMapper.selectById(showtime.getMovieId());
        if (movie != null) {
            ShowtimeVO.MovieBriefVO movieBrief = new ShowtimeVO.MovieBriefVO();
            movieBrief.setId(movie.getId());
            movieBrief.setName(movie.getName());
            vo.setMovie(movieBrief);
        }

        Hall hall = hallMapper.selectById(showtime.getHallId());
        if (hall != null) {
            ShowtimeVO.HallBriefVO hallBrief = new ShowtimeVO.HallBriefVO();
            hallBrief.setId(hall.getId());
            hallBrief.setName(hall.getName());
            hallBrief.setHallType(hall.getHallType() != null ? hall.getHallType().getCode() : null);
            vo.setHall(hallBrief);

            Cinema cinema = cinemaMapper.selectById(hall.getCinemaId());
            if (cinema != null) {
                ShowtimeVO.CinemaBriefVO cinemaBrief = new ShowtimeVO.CinemaBriefVO();
                cinemaBrief.setId(cinema.getId());
                cinemaBrief.setName(cinema.getName());
                vo.setCinema(cinemaBrief);
            }
        }

        vo.setStatus(showtime.getStatus() != null ? showtime.getStatus().getCode() : null);
        vo.setStatusDesc(showtime.getStatus() != null ? showtime.getStatus().getDesc() : null);
        // TODO: 从 showtime_seat 表统计
        vo.setSoldSeats(0);
        vo.setTotalSeats(0);
        vo.setLockedCount(0);
        return vo;
    }
}
