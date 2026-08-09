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
import com.szml.movieticket.entity.Seat;
import com.szml.movieticket.entity.Showtime;
import com.szml.movieticket.entity.ShowtimeSeat;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.enums.CinemaStatus;
import com.szml.movieticket.enums.HallStatus;
import com.szml.movieticket.enums.MovieStatus;
import com.szml.movieticket.enums.ShowtimeStatus;
import com.szml.movieticket.exception.ShowtimeException;
import com.szml.movieticket.mapper.CinemaMapper;
import com.szml.movieticket.mapper.HallMapper;
import com.szml.movieticket.mapper.MovieMapper;
import com.szml.movieticket.mapper.SeatMapper;
import com.szml.movieticket.mapper.ShowtimeMapper;
import com.szml.movieticket.mapper.ShowtimeSeatMapper;
import com.szml.movieticket.service.ShowtimeService;
import cn.hutool.json.JSONUtil;
import com.szml.movieticket.vo.ShowtimeGroupedVO;
import com.szml.movieticket.vo.ShowtimePageVO;
import com.szml.movieticket.vo.ShowtimeSeatLayoutVO;
import com.szml.movieticket.vo.ShowtimeSeatStatusVO;
import com.szml.movieticket.vo.ShowtimeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final int INVENTORY_AVAILABLE = 0;
    private static final int INVENTORY_LOCKED = 1;
    private static final int INVENTORY_SOLD = 2;
    private static final int INVENTORY_UNAVAILABLE = 3;
    private static final int INVENTORY_COUPLE = 4;

    private final MovieMapper movieMapper;
    private final HallMapper hallMapper;
    private final CinemaMapper cinemaMapper;
    private final SeatMapper seatMapper;
    private final ShowtimeSeatMapper showtimeSeatMapper;

    @Override
    @Transactional
    public ShowtimePageVO pageShowtimes(int page, int size, Long movieId, Long cinemaId, String date, String status) {
        //根据电影id,影院id,开场时间,以及场次状态进行查询相关列表
        LambdaQueryWrapper<Showtime> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Showtime::getStatus, ShowtimeStatus.ENDED);
        if (movieId != null) {
            wrapper.eq(Showtime::getMovieId, movieId);
        }
        if (cinemaId != null) {
            // cinemaId 在 hall 表，需先查出该影院的所有影厅ID
            List<Hall> halls = hallMapper.selectList(new LambdaQueryWrapper<Hall>().eq(Hall::getCinemaId, cinemaId));
            List<Long> hallIds = halls.stream().map(Hall::getId).collect(Collectors.toList());//根据影院id查询出所有的影厅ids
            if (hallIds.isEmpty()) {
                ShowtimePageVO empty = new ShowtimePageVO();
                empty.setTotal(0);
                empty.setPage(page);
                empty.setSize(size);
                empty.setRecords(new ArrayList<>());
                return empty;
            }
            wrapper.in(Showtime::getHallId, hallIds);//构建出影厅id的查询条件
        }
        if (StringUtils.hasText(date)) {
            LocalDateTime start = LocalDateTime.parse(date + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(date + "T23:59:59");
            wrapper.between(Showtime::getStartAt, start, end);//根据查询时间构造出查询条件
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Showtime::getStatus, ShowtimeStatus.fromCode(Integer.parseInt(status)));
        }
        wrapper.orderByDesc(Showtime::getStartAt);//按照开场时间进行降序排序

        Page<Showtime> pageResult = page(new Page<>(page, size), wrapper);
        List<ShowtimeVO> records = buildShowtimeVOList(pageResult.getRecords());

        ShowtimePageVO pageVO = new ShowtimePageVO();
        pageVO.setTotal(pageResult.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setRecords(records);
        return pageVO;
    }

    @Override
    @Transactional
    public void createShowtime(ShowtimeCreateDTO dto) {
        Movie movie = movieMapper.selectById(dto.getMovieId());
        if (movie == null) {
            throw new ShowtimeException(ErrorCode.MOVIE_NOT_FOUND);
        }
        if (movie.getStatus() == MovieStatus.OFFLINE) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_MOVIE_OFFLINE);
        }

        Hall hall = hallMapper.selectById(dto.getHallId());
        if (hall == null) {
            throw new ShowtimeException(ErrorCode.HALL_NOT_FOUND);
        }
        if (hall.getStatus() == HallStatus.INACTIVE) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_HALL_INACTIVE);
        }
        if (seatMapper.selectCount(new LambdaQueryWrapper<Seat>().eq(Seat::getHallId, dto.getHallId())) == 0) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_HALL_NO_SEATS);
        }

        Cinema cinema = cinemaMapper.selectById(hall.getCinemaId());
        if (cinema != null && cinema.getStatus() == CinemaStatus.INACTIVE) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_CINEMA_INACTIVE);
        }

        // 场次时间不能早于影片上映日期
        if (movie.getReleaseDate() != null && dto.getStartAt().toLocalDate().isBefore(movie.getReleaseDate())) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_BEFORE_RELEASE_DATE);
        }

        // 只允许创建明天及之后的场次
        LocalDateTime tomorrow = LocalDate.now().plusDays(1).atStartOfDay();
        if (dto.getStartAt().isBefore(tomorrow)) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_START_AT_TOO_EARLY);
        }

        // 计算散场时间
        LocalDateTime startAt = dto.getStartAt();
        LocalDateTime endAt = startAt.plusMinutes(movie.getDuration() + CLEANING_MINUTES);//加上10分钟

        // 时间冲突校验：同一影厅，新场次不能与已有场次重叠且间隔 ≥ 20min
        checkTimeConflict(dto.getHallId(), null, startAt, endAt);

        Showtime showtime = new Showtime();
        BeanUtils.copyProperties(dto, showtime);
        showtime.setEndAt(endAt);
        showtime.setLanguage(StringUtils.hasText(dto.getLanguage()) ? dto.getLanguage() : "国语2D");
        showtime.setStatus(ShowtimeStatus.ON_SALE);
        save(showtime);

        initializeShowtimeSeats(showtime);
        log.info("场次新增成功, id: {}, movieId: {}, hallId: {}, startAt: {}", showtime.getId(), dto.getMovieId(), dto.getHallId(), startAt);
    }

    @Override
    public void updateShowtime(Long id, ShowtimeUpdateDTO dto) {
        Showtime showtime = getById(id);
        if (showtime == null) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_NOT_FOUND);
        }

        List<String> updatedFields = new ArrayList<>();

        if (dto.getStartAt() != null) {
            // 有已锁定或已售座位时不允许修改时间
            long lockedOrSold = showtimeSeatMapper.selectCount(
                    new LambdaQueryWrapper<ShowtimeSeat>()
                            .eq(ShowtimeSeat::getShowtimeId, id)
                            .in(ShowtimeSeat::getStatus, INVENTORY_LOCKED, INVENTORY_SOLD));
            if (lockedOrSold > 0) {
                throw new ShowtimeException(ErrorCode.SHOWTIME_HAS_LOCKED_SEATS);
            }

            // 只允许改为明天及之后的场次
            LocalDateTime tomorrow = LocalDate.now().plusDays(1).atStartOfDay();
            if (dto.getStartAt().isBefore(tomorrow)) {
                throw new ShowtimeException(ErrorCode.SHOWTIME_START_AT_TOO_EARLY);
            }

            // 重新计算 endAt
            Movie movie = movieMapper.selectById(showtime.getMovieId());
            if (movie != null && movie.getReleaseDate() != null
                    && dto.getStartAt().toLocalDate().isBefore(movie.getReleaseDate())) {
                throw new ShowtimeException(ErrorCode.SHOWTIME_BEFORE_RELEASE_DATE);
            }
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
    }

    @Override
    public void updateShowtimeStatus(Long id, ShowtimeStatusDTO dto) {
        Showtime showtime = getById(id);
        if (showtime == null) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_NOT_FOUND);
        }

        // 售罄为终态，不允许再变更
        if (showtime.getStatus() == ShowtimeStatus.SOLD_OUT_ALL) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_HAS_LOCKED_SEATS);
        }

        // 有已锁定座位时不允许变更状态（已售不影响，管理员可以手动停售）
        long lockedCount = showtimeSeatMapper.selectCount(
                new LambdaQueryWrapper<ShowtimeSeat>()
                        .eq(ShowtimeSeat::getShowtimeId, id)
                        .eq(ShowtimeSeat::getStatus, INVENTORY_LOCKED));
        if (lockedCount > 0) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_HAS_LOCKED_SEATS);
        }

        showtime.setStatus(dto.getStatus());
        updateById(showtime);

        log.info("场次状态变更, id: {}, newStatus: {}", id, dto.getStatus());
    }

    @Override
    @Transactional
    public ShowtimeSeatStatusVO updateSeatStatus(Long showtimeId, ShowtimeSeatStatusDTO dto) {
        if (dto == null || (!"AVAILABLE".equals(dto.getStatus())
                && !"UNAVAILABLE".equals(dto.getStatus()))) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_SEAT_STATUS_INVALID);
        }
        if (dto.getSeatIds() == null || dto.getSeatIds().isEmpty()) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_SEAT_NOT_FOUND);
        }
        Showtime showtime = getById(showtimeId);
        if (showtime == null) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_NOT_FOUND);
        }

        List<Long> updatedSeatIds = new ArrayList<>();
        List<Long> skippedSeatIds = new ArrayList<>();

        List<Long> requestedSeatIds = dto.getSeatIds().stream().distinct().toList();
        List<ShowtimeSeat> seats = showtimeSeatMapper.selectList(
                new LambdaQueryWrapper<ShowtimeSeat>()
                        .eq(ShowtimeSeat::getShowtimeId, showtimeId)
                        .in(ShowtimeSeat::getId, requestedSeatIds));
        if (seats.size() != requestedSeatIds.size()) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_SEAT_NOT_FOUND);
        }

        Map<Long, Seat> physicalSeats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                        .in(Seat::getId, seats.stream().map(ShowtimeSeat::getSeatId).toList()))
                .stream()
                .collect(Collectors.toMap(Seat::getId, seat -> seat));

        for (ShowtimeSeat seat : seats) {
            Seat physicalSeat = physicalSeats.get(seat.getSeatId());
            int currentStatus = seat.getStatus() == null
                    ? (physicalSeat == null ? INVENTORY_UNAVAILABLE : inventoryStatus(physicalSeat))
                    : seat.getStatus();
            if ("UNAVAILABLE".equals(dto.getStatus())) {
                if (physicalSeat == null || currentStatus == INVENTORY_SOLD
                        || currentStatus == INVENTORY_LOCKED || currentStatus == INVENTORY_UNAVAILABLE) {
                    skippedSeatIds.add(seat.getId());
                    continue;
                }
                seat.setStatus(INVENTORY_UNAVAILABLE);
                updatedSeatIds.add(seat.getId());
            } else if ("AVAILABLE".equals(dto.getStatus())) {
                if (currentStatus == INVENTORY_SOLD || currentStatus == INVENTORY_LOCKED
                        || physicalSeat == null || (physicalSeat.getStatus() != null && physicalSeat.getStatus() == 1)
                        || (currentStatus != INVENTORY_UNAVAILABLE
                        && currentStatus != INVENTORY_AVAILABLE
                        && currentStatus != INVENTORY_COUPLE)) {
                    skippedSeatIds.add(seat.getId());
                    continue;
                }
                if (currentStatus == INVENTORY_UNAVAILABLE) {
                    seat.setStatus(inventoryStatus(physicalSeat));
                    updatedSeatIds.add(seat.getId());
                } else {
                    skippedSeatIds.add(seat.getId());
                }
            }
        }

        for (ShowtimeSeat seat : seats) {
            if (updatedSeatIds.contains(seat.getId())) {
                showtimeSeatMapper.updateById(seat);
            }
        }

        log.info("座位状态批量更新, showtimeId: {}, targetStatus: {}, updated: {}, skipped: {}",
                showtimeId, dto.getStatus(), updatedSeatIds.size(), skippedSeatIds.size());

        ShowtimeSeatStatusVO result = new ShowtimeSeatStatusVO();
        result.setUpdatedSeatIds(updatedSeatIds);
        result.setSkippedSeatIds(skippedSeatIds);
        if (!skippedSeatIds.isEmpty()) {
            result.setSkippedReason("座位已被售出或锁定，不可修改状态");
        }
        return result;
    }

    @Override
    @Transactional
    public ShowtimeSeatLayoutVO getSeatLayout(Long showtimeId) {
        Showtime showtime = getById(showtimeId);
        if (showtime == null) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_NOT_FOUND);
        }

        Hall hall = hallMapper.selectById(showtime.getHallId());
        if (hall == null) {
            throw new ShowtimeException(ErrorCode.HALL_NOT_FOUND);
        }
        Movie movie = movieMapper.selectById(showtime.getMovieId());
        Cinema cinema = cinemaMapper.selectById(hall.getCinemaId());

        List<Seat> physicalSeats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getHallId, hall.getId())
                .orderByAsc(Seat::getRowNo)
                .orderByAsc(Seat::getSeatNo));
        List<ShowtimeSeat> inventories = showtimeSeatMapper.selectList(new LambdaQueryWrapper<ShowtimeSeat>()
                .eq(ShowtimeSeat::getShowtimeId, showtimeId));
        Map<Long, ShowtimeSeat> inventoryByPhysicalId = inventories.stream()
                .collect(Collectors.toMap(ShowtimeSeat::getSeatId, seat -> seat));

        Map<Integer, List<ShowtimeSeatLayoutVO.SeatVO>> rowMap = new LinkedHashMap<>();
        int availableSeats = 0;
        int lockedSeats = 0;
        int soldSeats = 0;
        int unavailableSeats = 0;
        for (Seat physicalSeat : physicalSeats) {
            ShowtimeSeat inventory = inventoryByPhysicalId.get(physicalSeat.getId());
            if (inventory == null) {
                continue;
            }
            int status = inventory.getStatus() == null
                    ? inventoryStatus(physicalSeat)
                    : inventory.getStatus();
            if (status == INVENTORY_AVAILABLE || status == INVENTORY_COUPLE) {
                availableSeats++;
            } else if (status == INVENTORY_LOCKED) {
                lockedSeats++;
            } else if (status == INVENTORY_SOLD) {
                soldSeats++;
            } else if (status == INVENTORY_UNAVAILABLE) {
                unavailableSeats++;
            }

            ShowtimeSeatLayoutVO.SeatVO seatVO = new ShowtimeSeatLayoutVO.SeatVO();
            seatVO.setId(inventory.getId());
            seatVO.setPhysicalSeatId(physicalSeat.getId());
            seatVO.setSeatNo(physicalSeat.getSeatNo());
            seatVO.setZone(physicalSeat.getZone());
            seatVO.setSeatType(physicalSeat.getSeatType() != null && physicalSeat.getSeatType() == 1
                    ? "COUPLE" : "NORMAL");
            seatVO.setStatus(inventoryStatusName(status));
            seatVO.setPrice(inventory.getPrice() != null ? inventory.getPrice() : showtime.getBasePrice());
            rowMap.computeIfAbsent(physicalSeat.getRowNo(), key -> new ArrayList<>()).add(seatVO);
        }

        List<ShowtimeSeatLayoutVO.RowVO> rows = new ArrayList<>();
        rowMap.forEach((rowNo, seats) -> {
            ShowtimeSeatLayoutVO.RowVO row = new ShowtimeSeatLayoutVO.RowVO();
            row.setRowNo(rowNo);
            row.setSeats(seats);
            rows.add(row);
        });

        ShowtimeSeatLayoutVO result = new ShowtimeSeatLayoutVO();
        result.setShowtimeId(showtimeId);
        result.setMovieName(movie != null ? movie.getName() : null);
        result.setCinemaName(cinema != null ? cinema.getName() : null);
        result.setHallName(hall.getName());
        result.setHallType(hall.getHallType() != null ? hall.getHallType().getCode() : null);
        result.setStartAt(showtime.getStartAt());
        result.setBasePrice(showtime.getBasePrice());
        result.setTotalSeats(physicalSeats.size());
        result.setAvailableSeats(availableSeats);
        result.setLockedSeats(lockedSeats);
        result.setSoldSeats(soldSeats);
        result.setUnavailableSeats(unavailableSeats);
        result.setRows(rows);
        return result;
    }

    @Override
    public ShowtimeSeatLayoutVO getSeatLayoutForUser(Long showtimeId) {
        Showtime showtime = getById(showtimeId);
        if (showtime == null) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_NOT_FOUND);
        }
        if (showtime.getStartAt() == null || !showtime.getStartAt().isAfter(LocalDateTime.now())) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_ALREADY_STARTED);
        }
        return getSeatLayout(showtimeId);
    }

    @Override
    public ShowtimeGroupedVO listShowtimesForUser(Long movieId, Long cinemaId, String date, String hallType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.toLocalDate().atStartOfDay();
        LocalDateTime endDate = startDate.plusDays(1);
        if (StringUtils.hasText(date)) {
            startDate = LocalDateTime.parse(date + "T00:00:00");
            endDate = LocalDateTime.parse(date + "T23:59:59");
        }

        // 查询影院信息
        Cinema cinema = null;
        if (cinemaId != null) {
            cinema = cinemaMapper.selectById(cinemaId);
        }

        // 查询在售场次
        LambdaQueryWrapper<Showtime> wrapper = new LambdaQueryWrapper<>();
        if (movieId != null) {
            wrapper.eq(Showtime::getMovieId, movieId);
        }
        if (cinemaId != null) {
            List<Hall> halls = hallMapper.selectList(
                    new LambdaQueryWrapper<Hall>().eq(Hall::getCinemaId, cinemaId));
            List<Long> hallIds = halls.stream().map(Hall::getId).toList();
            if (hallIds.isEmpty()) {
                ShowtimeGroupedVO emptyResult = new ShowtimeGroupedVO();
                emptyResult.setMovies(new ArrayList<>());
                return emptyResult;
            }
            wrapper.in(Showtime::getHallId, hallIds);
        }
        LocalDateTime effectiveStart = startDate.isAfter(now) ? startDate : now;
        wrapper.eq(Showtime::getStatus, ShowtimeStatus.ON_SALE)
                .gt(Showtime::getStartAt, effectiveStart)
                .le(Showtime::getStartAt, endDate);

        if (StringUtils.hasText(hallType)) {
            List<Hall> halls = hallMapper.selectList(
                    new LambdaQueryWrapper<Hall>().eq(Hall::getHallType, hallType));
            List<Long> hallIds = halls.stream().map(Hall::getId).toList();
            wrapper.in(Showtime::getHallId, hallIds);
        }
        wrapper.orderByAsc(Showtime::getStartAt);

        List<Showtime> showtimes = list(wrapper);
        Map<Long, Hall> showtimeHallMap = new HashMap<>();
        Map<Long, Cinema> showtimeCinemaMap = new HashMap<>();
        if (!showtimes.isEmpty()) {
            Set<Long> showtimeHallIds = showtimes.stream()
                    .map(Showtime::getHallId)
                    .collect(Collectors.toSet());
            if (!showtimeHallIds.isEmpty()) {
                hallMapper.selectBatchIds(showtimeHallIds)
                        .forEach(hall -> showtimeHallMap.put(hall.getId(), hall));
            }

            Set<Long> showtimeCinemaIds = showtimeHallMap.values().stream()
                    .map(Hall::getCinemaId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            if (!showtimeCinemaIds.isEmpty()) {
                cinemaMapper.selectBatchIds(showtimeCinemaIds)
                        .forEach(item -> showtimeCinemaMap.put(item.getId(), item));
            }
        }

        // 按影片ID分组
        Map<Long, List<Showtime>> groupedByMovie = new LinkedHashMap<>();
        for (Showtime showtime : showtimes) {
            groupedByMovie.computeIfAbsent(showtime.getMovieId(), k -> new ArrayList<>()).add(showtime);
        }

        // 构建响应
        ShowtimeGroupedVO vo = new ShowtimeGroupedVO();
        if (cinema != null) {
            ShowtimeGroupedVO.CinemaBrief cinemaBrief = new ShowtimeGroupedVO.CinemaBrief();
            cinemaBrief.setId(cinema.getId());
            cinemaBrief.setName(cinema.getName());
            cinemaBrief.setAddress(cinema.getAddress());
            cinemaBrief.setServices(JSONUtil.toList(cinema.getServices(), String.class));
            vo.setCinema(cinemaBrief);
        }

        List<ShowtimeGroupedVO.MovieGroup> movies = new ArrayList<>();
        for (Map.Entry<Long, List<Showtime>> entry : groupedByMovie.entrySet()) {
            Movie movie = movieMapper.selectById(entry.getKey());
            if (movie == null) continue;

            ShowtimeGroupedVO.MovieGroup group = new ShowtimeGroupedVO.MovieGroup();
            group.setId(movie.getId());
            group.setName(movie.getName());
            group.setPoster(movie.getPoster());
            group.setDuration(movie.getDuration());

            List<ShowtimeGroupedVO.ShowtimeItem> items = new ArrayList<>();
            for (Showtime showtime : entry.getValue()) {
                ShowtimeGroupedVO.ShowtimeItem item = new ShowtimeGroupedVO.ShowtimeItem();
                item.setId(showtime.getId());
                item.setStartAt(showtime.getStartAt());
                item.setEndAt(showtime.getEndAt());
                item.setLanguage(showtime.getLanguage());
                item.setBasePrice(showtime.getBasePrice());

                Hall hall = showtimeHallMap.get(showtime.getHallId());
                if (hall != null) {
                    item.setHallName(hall.getName());
                    item.setHallType(hall.getHallType() != null ? hall.getHallType().getCode() : null);
                    item.setCinemaId(hall.getCinemaId());
                    Cinema itemCinema = showtimeCinemaMap.get(hall.getCinemaId());
                    if (itemCinema != null) {
                        item.setCinemaName(itemCinema.getName());
                    }
                }

                // 统计剩余座位
                List<ShowtimeSeat> seats = showtimeSeatMapper.selectList(
                        new LambdaQueryWrapper<ShowtimeSeat>()
                                .eq(ShowtimeSeat::getShowtimeId, showtime.getId()));
                long remaining = seats.stream()
                        .filter(s -> s.getStatus() == null || s.getStatus() == INVENTORY_AVAILABLE || s.getStatus() == INVENTORY_COUPLE)
                        .count();
                item.setRemainingSeats((int) remaining);
                item.setTotalSeats(seats.size());
                item.setStatus(showtime.getStatus() != null ? showtime.getStatus().getDesc() : null);

                items.add(item);
            }
            group.setShowtimes(items);
            movies.add(group);
        }

        vo.setMovies(movies);
        return vo;
    }

    /**
     * 校验同一影厅场次时间冲突：不可重叠，间隔 ≥ 20 分钟。
     */
    private void checkTimeConflict(Long hallId, Long excludeShowtimeId, LocalDateTime startAt, LocalDateTime endAt) {
        // startAt 预留 10 分钟清洁/入场间隔，endAt 在 createShowtime 中已包含清洁时间

        LambdaQueryWrapper<Showtime> wrapper = new LambdaQueryWrapper<Showtime>()
                .eq(Showtime::getHallId, hallId)
                .ne(Showtime::getStatus, ShowtimeStatus.ENDED)
                .gt(Showtime::getEndAt, startAt)
                .lt(Showtime::getStartAt, endAt);
        if (excludeShowtimeId != null) {
            wrapper.ne(Showtime::getId, excludeShowtimeId);
        }

        long count = count(wrapper);
        if (count > 0) {
            throw new ShowtimeException(ErrorCode.SHOWTIME_TIME_CONFLICT);
        }
    }

    private List<ShowtimeVO> buildShowtimeVOList(List<Showtime> showtimes) {
        if (showtimes.isEmpty()) {
            return List.of();
        }

        // 收集 IDs
        Set<Long> movieIds = new HashSet<>();
        Set<Long> hallIds = new HashSet<>();
        Set<Long> showtimeIds = new HashSet<>();
        for (Showtime s : showtimes) {
            movieIds.add(s.getMovieId());
            hallIds.add(s.getHallId());
            showtimeIds.add(s.getId());
        }

        // 1. 批量查 Movie
        Map<Long, Movie> movieMap = new HashMap<>();
        if (!movieIds.isEmpty()) {
            movieMapper.selectBatchIds(movieIds)
                    .forEach(m -> movieMap.put(m.getId(), m));
        }

        // 2. 批量查 Hall
        Map<Long, Hall> hallMap = new HashMap<>();
        if (!hallIds.isEmpty()) {
            hallMapper.selectBatchIds(hallIds)
                    .forEach(h -> hallMap.put(h.getId(), h));
        }

        // 3. 批量查 Cinema
        Set<Long> cinemaIds = new HashSet<>();
        for (Hall h : hallMap.values()) {
            cinemaIds.add(h.getCinemaId());
        }
        Map<Long, Cinema> cinemaMap = new HashMap<>();
        if (!cinemaIds.isEmpty()) {
            cinemaMapper.selectBatchIds(cinemaIds)
                    .forEach(c -> cinemaMap.put(c.getId(), c));
        }

        // 4. 批量查座位数
        Map<Long, Integer> seatCountMap = new HashMap<>();
        if (!hallIds.isEmpty()) {
            List<Seat> allSeats = seatMapper.selectList(
                    new LambdaQueryWrapper<Seat>().in(Seat::getHallId, hallIds));
            for (Seat seat : allSeats) {
                seatCountMap.merge(seat.getHallId(), 1, Integer::sum);
            }
        }

        // 5. 批量查库存
        Map<Long, List<ShowtimeSeat>> inventoryMap = new HashMap<>();
        if (!showtimeIds.isEmpty()) {
            List<ShowtimeSeat> allInventories = showtimeSeatMapper.selectList(
                    new LambdaQueryWrapper<ShowtimeSeat>().in(ShowtimeSeat::getShowtimeId, showtimeIds));
            for (ShowtimeSeat inv : allInventories) {
                inventoryMap.computeIfAbsent(inv.getShowtimeId(), k -> new ArrayList<>()).add(inv);
            }
        }

        // 6. 组装
        return showtimes.stream().map(s -> {
            ShowtimeVO vo = new ShowtimeVO();
            BeanUtils.copyProperties(s, vo);

            Movie movie = movieMap.get(s.getMovieId());
            if (movie != null) {
                ShowtimeVO.MovieBriefVO mb = new ShowtimeVO.MovieBriefVO();
                mb.setId(movie.getId());
                mb.setName(movie.getName());
                vo.setMovie(mb);
            }

            Hall hall = hallMap.get(s.getHallId());
            if (hall != null) {
                ShowtimeVO.HallBriefVO hb = new ShowtimeVO.HallBriefVO();
                hb.setId(hall.getId());
                hb.setName(hall.getName());
                hb.setHallType(hall.getHallType() != null ? hall.getHallType().getCode() : null);
                vo.setHall(hb);

                Cinema cinema = cinemaMap.get(hall.getCinemaId());
                if (cinema != null) {
                    ShowtimeVO.CinemaBriefVO cb = new ShowtimeVO.CinemaBriefVO();
                    cb.setId(cinema.getId());
                    cb.setName(cinema.getName());
                    vo.setCinema(cb);
                }
            }

            vo.setStatus(s.getStatus() != null ? s.getStatus().getCode() : null);
            vo.setStatusDesc(s.getStatus() != null ? s.getStatus().getDesc() : null);

            int totalSeats = seatCountMap.getOrDefault(s.getHallId(), 0);
            vo.setTotalSeats(totalSeats);

            List<ShowtimeSeat> inventories = inventoryMap.getOrDefault(s.getId(), List.of());
            vo.setSoldSeats((int) inventories.stream()
                    .filter(seat -> seat.getStatus() != null && seat.getStatus() == INVENTORY_SOLD)
                    .count());

            return vo;
        }).collect(Collectors.toList());
    }

    private void initializeShowtimeSeats(Showtime showtime) {
        List<Seat> physicalSeats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getHallId, showtime.getHallId()));
        if (physicalSeats.isEmpty()) {
            return;
        }

        List<ShowtimeSeat> toInsert = new ArrayList<>();
        for (Seat physicalSeat : physicalSeats) {
            ShowtimeSeat inventory = new ShowtimeSeat();
            inventory.setShowtimeId(showtime.getId());
            inventory.setSeatId(physicalSeat.getId());
            inventory.setPrice(showtime.getBasePrice());
            inventory.setStatus(inventoryStatus(physicalSeat));
            inventory.setVersion(0);
            toInsert.add(inventory);
        }
        showtimeSeatMapper.insertBatch(toInsert);
        log.info("场次库存初始化完成, showtimeId: {}, seatCount: {}", showtime.getId(), toInsert.size());
    }

    private static int inventoryStatus(Seat seat) {
        if (seat.getStatus() != null && seat.getStatus() == 1) {
            return INVENTORY_UNAVAILABLE;//不可选状态
        }
        return seat.getSeatType() != null && seat.getSeatType() == 1
                ? INVENTORY_COUPLE : INVENTORY_AVAILABLE;//情侣座或者普通坐
    }

    private static String inventoryStatusName(int status) {
        return switch (status) {
            case INVENTORY_LOCKED -> "LOCKED";
            case INVENTORY_SOLD -> "SOLD";
            case INVENTORY_UNAVAILABLE -> "UNAVAILABLE";
            case INVENTORY_COUPLE -> "COUPLE";
            default -> "AVAILABLE";
        };
    }
}
