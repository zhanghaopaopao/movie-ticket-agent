package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.dto.HallCreateDTO;
import com.szml.movieticket.dto.HallStatusDTO;
import com.szml.movieticket.dto.HallUpdateDTO;
import com.szml.movieticket.dto.SeatCreateDTO;
import com.szml.movieticket.dto.SeatLayoutItemDTO;
import com.szml.movieticket.dto.SeatLayoutSaveDTO;
import com.szml.movieticket.dto.SeatUpdateDTO;
import com.szml.movieticket.entity.Cinema;
import com.szml.movieticket.entity.Hall;
import com.szml.movieticket.entity.Seat;
import com.szml.movieticket.entity.Showtime;
import com.szml.movieticket.entity.ShowtimeSeat;
import com.szml.movieticket.enums.HallStatus;
import com.szml.movieticket.enums.ShowtimeStatus;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.HallException;
import com.szml.movieticket.exception.SeatException;
import com.szml.movieticket.mapper.CinemaMapper;
import com.szml.movieticket.mapper.HallMapper;
import com.szml.movieticket.mapper.OrderItemMapper;
import com.szml.movieticket.mapper.SeatMapper;
import com.szml.movieticket.mapper.ShowtimeMapper;
import com.szml.movieticket.mapper.ShowtimeSeatMapper;
import com.szml.movieticket.service.HallService;
import com.szml.movieticket.vo.HallPageVO;
import com.szml.movieticket.vo.HallSeatVO;
import com.szml.movieticket.vo.HallVO;
import com.szml.movieticket.vo.SeatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 影厅服务实现类。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HallServiceImpl extends ServiceImpl<HallMapper, Hall> implements HallService {

    private static final Set<String> VALID_ZONES = Set.of("FRONT", "MIDDLE", "BACK", "COUPLE");

    private final CinemaMapper cinemaMapper;
    private final SeatMapper seatMapper;
    private final ShowtimeMapper showtimeMapper;
    private final ShowtimeSeatMapper showtimeSeatMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public HallPageVO pageHallsByCinemaId(int page, int size, Long cinemaId, String keyword) {
        //根据影院id以及关键字进行条件搜索
        LambdaQueryWrapper<Hall> wrapper = new LambdaQueryWrapper<Hall>()
                .eq(Hall::getCinemaId, cinemaId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Hall::getName, keyword);//模糊查询影厅名称
        }
        wrapper.orderByAsc(Hall::getCreateTime);//根据创建时间进行降序排序

        Page<Hall> pageResult = page(new Page<>(page, size), wrapper);
        List<HallVO> records = pageResult.getRecords().stream().map(this::toVO).collect(Collectors.toList());

        HallPageVO pageVO = new HallPageVO();
        pageVO.setTotal(pageResult.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setRecords(records);
        return pageVO;
    }

    @Override
    public void createHall(HallCreateDTO dto) {
        // 同一影院下名称唯一
        long count = count(new LambdaQueryWrapper<Hall>()
                .eq(Hall::getCinemaId, dto.getCinemaId())
                .eq(Hall::getName, dto.getName()));
        if (count > 0) {
            throw new HallException(ErrorCode.HALL_NAME_DUPLICATE);
        }

        Hall hall = new Hall();
        BeanUtils.copyProperties(dto, hall);
        hall.setStatus(HallStatus.ACTIVE);
        save(hall);

        log.info("影厅新增成功, id: {}, name: {}, cinemaId: {}", hall.getId(), hall.getName(), hall.getCinemaId());
    }

    @Override
    public void updateHall(Long id, HallUpdateDTO dto) {
        Hall hall = getById(id);
        if (hall == null) {
            throw new HallException(ErrorCode.HALL_NOT_FOUND);
        }

        List<String> updatedFields = new ArrayList<>();
        if (dto.getName() != null && !dto.getName().equals(hall.getName())) {
            long count = count(new LambdaQueryWrapper<Hall>()
                    .eq(Hall::getCinemaId, hall.getCinemaId())
                    .eq(Hall::getName, dto.getName()));
            if (count > 0) {
                throw new HallException(ErrorCode.HALL_NAME_DUPLICATE);
            }
            hall.setName(dto.getName());
            updatedFields.add("name");
        }
        if (dto.getHallType() != null) {
            hall.setHallType(dto.getHallType());
            updatedFields.add("hallType");
        }

        updateById(hall);

        log.info("影厅编辑成功, id: {}, updatedFields: {}", id, updatedFields);
    }

    @Override
    public void updateHallStatus(Long id, HallStatusDTO dto) {
        Hall hall = getById(id);
        if (hall == null) {
            throw new HallException(ErrorCode.HALL_NOT_FOUND);
        }

        if (dto.getStatus() == HallStatus.INACTIVE) {
            long activeCount = showtimeMapper.selectCount(
                    new LambdaQueryWrapper<Showtime>()
                            .eq(Showtime::getHallId, id)
                            .eq(Showtime::getStatus, ShowtimeStatus.ON_SALE));
            if (activeCount > 0) {
                throw new HallException(ErrorCode.HALL_HAS_ACTIVE_SHOWTIMES);
            }
        }

        hall.setStatus(dto.getStatus());
        updateById(hall);

        log.info("影厅状态变更, id: {}, newStatus: {}", id, dto.getStatus());
    }

    @Override
    public HallSeatVO getHallSeats(Long hallId) {
        Hall hall = getById(hallId);//查询影厅实体
        if (hall == null) {
            throw new HallException(ErrorCode.HALL_NOT_FOUND);
        }

        Cinema cinema = cinemaMapper.selectById(hall.getCinemaId());//查询影院实体

        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getHallId, hallId)
                .orderByAsc(Seat::getRowNo, Seat::getSeatNo));

        // 统计
        Map<String, Integer> summary = new HashMap<>();
        summary.put("totalSeats", seats.size());
        summary.put("normalSeats", (int) seats.stream().filter(s -> s.getSeatType() == 0).count());//普通坐
        summary.put("coupleSeats", (int) seats.stream().filter(s -> s.getSeatType() == 1).count());//情侣坐
        summary.put("unavailableSeats", (int) seats.stream().filter(s -> s.getStatus() == 1).count());//不可用坐

        // 按排分组
        Map<Integer, List<HallSeatVO.SeatItemVO>> rowMap = new HashMap<>();
        for (Seat seat : seats) {
            HallSeatVO.SeatItemVO item = new HallSeatVO.SeatItemVO();
            item.setId(seat.getId());
            item.setSeatNo(seat.getSeatNo());
            item.setZone(seat.getZone());
            item.setSeatType(seat.getSeatType() == 1 ? "COUPLE" : "NORMAL");
            item.setStatus(seat.getStatus() == 1 ? "UNAVAILABLE" : "AVAILABLE");
            rowMap.computeIfAbsent(seat.getRowNo(), k -> new ArrayList<>()).add(item);
        }

        List<HallSeatVO.RowVO> rows = new ArrayList<>();
        for (Map.Entry<Integer, List<HallSeatVO.SeatItemVO>> entry : rowMap.entrySet()) {
            HallSeatVO.RowVO rowVO = new HallSeatVO.RowVO();
            rowVO.setRowNo(entry.getKey());
            rowVO.setSeats(entry.getValue());
            rows.add(rowVO);
        }

        HallSeatVO seatVO = new HallSeatVO();
        seatVO.setHallId(hall.getId());
        seatVO.setHallName(hall.getName());
        seatVO.setHallType(hall.getHallType() != null ? hall.getHallType().getDesc() : null);
        seatVO.setCinemaName(cinema != null ? cinema.getName() : null);
        seatVO.setSummary(summary);
        seatVO.setRows(rows);

        return seatVO;
    }

    @Override
    @Transactional
    public SeatVO createSeat(Long hallId, SeatCreateDTO dto) {
        requireHall(hallId);
        Integer status = dto.getStatus() == null ? 0 : dto.getStatus();
        validateSeatValues(dto.getRowNo(), dto.getSeatNo(), dto.getZone(), dto.getSeatType(), status);
        ensureUniquePosition(hallId, null, dto.getRowNo(), dto.getSeatNo());

        Seat seat = new Seat();
        seat.setHallId(hallId);
        seat.setRowNo(dto.getRowNo());
        seat.setSeatNo(dto.getSeatNo());
        seat.setZone(normalizeZone(dto.getZone()));
        seat.setSeatType(dto.getSeatType());
        seat.setStatus(status);
        seatMapper.insert(seat);

        syncSeatToShowtimes(seat);
        log.info("物理座位新增成功, hallId: {}, seatId: {}, position: {}-{}",
                hallId, seat.getId(), seat.getRowNo(), seat.getSeatNo());
        return toSeatVO(seat);
    }

    @Override
    @Transactional
    public SeatVO updateSeat(Long hallId, Long seatId, SeatUpdateDTO dto) {
        requireHall(hallId);
        Seat seat = getSeat(hallId, seatId);
        Integer rowNo = dto.getRowNo() == null ? seat.getRowNo() : dto.getRowNo();
        Integer seatNo = dto.getSeatNo() == null ? seat.getSeatNo() : dto.getSeatNo();
        String zone = dto.getZone() == null ? seat.getZone() : normalizeZone(dto.getZone());
        Integer seatType = dto.getSeatType() == null ? seat.getSeatType() : dto.getSeatType();
        Integer status = dto.getStatus() == null ? seat.getStatus() : dto.getStatus();

        validateSeatValues(rowNo, seatNo, zone, seatType, status);
        ensureUniquePosition(hallId, seatId, rowNo, seatNo);

        boolean changed = !Objects.equals(seat.getRowNo(), rowNo)
                || !Objects.equals(seat.getSeatNo(), seatNo)
                || !Objects.equals(seat.getZone(), zone)
                || !Objects.equals(seat.getSeatType(), seatType)
                || !Objects.equals(seat.getStatus(), status);
        if (!changed) {
            return toSeatVO(seat);
        }

        assertSeatCanChange(seatId);
        seat.setRowNo(rowNo);
        seat.setSeatNo(seatNo);
        seat.setZone(zone);
        seat.setSeatType(seatType);
        seat.setStatus(status);
        seatMapper.updateById(seat);
        syncSeatToShowtimes(seat);
        log.info("物理座位编辑成功, hallId: {}, seatId: {}", hallId, seatId);
        return toSeatVO(seat);
    }

    @Override
    @Transactional
    public void deleteSeat(Long hallId, Long seatId) {
        requireHall(hallId);
        Seat seat = getSeat(hallId, seatId);
        assertSeatCanChange(seatId);
        showtimeSeatMapper.delete(new LambdaQueryWrapper<ShowtimeSeat>()
                .eq(ShowtimeSeat::getSeatId, seatId));
        seatMapper.deleteById(seat.getId());
        log.info("物理座位删除成功, hallId: {}, seatId: {}", hallId, seatId);
    }

    @Override
    @Transactional
    public HallSeatVO saveSeatLayout(Long hallId, SeatLayoutSaveDTO dto) {
        requireHall(hallId);
        List<SeatLayoutItemDTO> items = dto.getSeats() == null ? List.of() : dto.getSeats();
        List<Seat> existingSeats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getHallId, hallId));
        Map<Long, Seat> existingById = existingSeats.stream()
                .collect(Collectors.toMap(Seat::getId, seat -> seat));
        Map<String, Seat> existingByPosition = existingSeats.stream()
                .collect(Collectors.toMap(
                        seat -> positionKey(seat.getRowNo(), seat.getSeatNo()),
                        seat -> seat));
        Map<String, SeatLayoutItemDTO> positions = new HashMap<>();
        Set<Long> incomingIds = new HashSet<>();

        for (SeatLayoutItemDTO item : items) {
            Integer status = item.getStatus() == null ? 0 : item.getStatus();
            String zone = normalizeZone(item.getZone());
            validateSeatValues(item.getRowNo(), item.getSeatNo(), zone, item.getSeatType(), status);
            String position = positionKey(item.getRowNo(), item.getSeatNo());
            if (positions.put(position, item) != null) {
                throw new SeatException(ErrorCode.SEAT_POSITION_DUPLICATE);
            }
            Seat existingAtPosition = existingByPosition.get(position);
            if (existingAtPosition != null && !Objects.equals(existingAtPosition.getId(), item.getId())) {
                throw new SeatException(ErrorCode.SEAT_POSITION_DUPLICATE);
            }
            if (item.getId() != null) {
                if (!incomingIds.add(item.getId()) || !existingById.containsKey(item.getId())) {
                    throw new SeatException(ErrorCode.SEAT_NOT_FOUND);
                }
            }
        }

        for (Seat existing : existingSeats) {
            SeatLayoutItemDTO item = items.stream()
                    .filter(candidate -> Objects.equals(candidate.getId(), existing.getId()))
                    .findFirst().orElse(null);
            if (item == null || seatChanged(existing, item)) {
                assertSeatCanChange(existing.getId());
            }
        }

        Set<Long> retainedIds = new HashSet<>();
        for (SeatLayoutItemDTO item : items) {
            Integer status = item.getStatus() == null ? 0 : item.getStatus();
            String zone = normalizeZone(item.getZone());
            if (item.getId() == null) {
                Seat seat = new Seat();
                seat.setHallId(hallId);
                seat.setRowNo(item.getRowNo());
                seat.setSeatNo(item.getSeatNo());
                seat.setZone(zone);
                seat.setSeatType(item.getSeatType());
                seat.setStatus(status);
                seatMapper.insert(seat);
                syncSeatToShowtimes(seat);
            } else {
                Seat seat = existingById.get(item.getId());
                retainedIds.add(seat.getId());
                if (seatChanged(seat, item)) {
                    seat.setRowNo(item.getRowNo());
                    seat.setSeatNo(item.getSeatNo());
                    seat.setZone(zone);
                    seat.setSeatType(item.getSeatType());
                    seat.setStatus(status);
                    seatMapper.updateById(seat);
                    syncSeatToShowtimes(seat);
                }
            }
        }

        for (Seat existing : existingSeats) {
            if (!retainedIds.contains(existing.getId()) && !items.stream()
                    .anyMatch(item -> Objects.equals(item.getId(), existing.getId()))) {
                showtimeSeatMapper.delete(new LambdaQueryWrapper<ShowtimeSeat>()
                        .eq(ShowtimeSeat::getSeatId, existing.getId()));
                seatMapper.deleteById(existing.getId());
            }
        }
        return getHallSeats(hallId);
    }

    private HallVO toVO(Hall hall) {
        HallVO vo = new HallVO();
        BeanUtils.copyProperties(hall, vo);
        vo.setHallType(hall.getHallType() != null ? hall.getHallType().getCode() : null);
        vo.setHallTypeDesc(hall.getHallType() != null ? hall.getHallType().getDesc() : null);
        vo.setStatus(hall.getStatus() != null ? hall.getStatus().getCode() : null);
        vo.setStatusDesc(hall.getStatus() != null ? hall.getStatus().getDesc() : null);
        // 统计座位数
        long seatCount = seatMapper.selectCount(new LambdaQueryWrapper<Seat>().eq(Seat::getHallId, hall.getId()));
        vo.setTotalSeats((int) seatCount);
        return vo;
    }

    private Hall requireHall(Long hallId) {
        Hall hall = getById(hallId);
        if (hall == null) {
            throw new HallException(ErrorCode.HALL_NOT_FOUND);
        }
        return hall;
    }

    private Seat getSeat(Long hallId, Long seatId) {
        Seat seat = seatMapper.selectOne(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getId, seatId)
                .eq(Seat::getHallId, hallId));
        if (seat == null) {
            throw new SeatException(ErrorCode.SEAT_NOT_FOUND);
        }
        return seat;
    }

    private void validateSeatValues(Integer rowNo, Integer seatNo, String zone,
                                    Integer seatType, Integer status) {
        if (rowNo == null || rowNo < 1 || seatNo == null || seatNo < 1) {
            throw new SeatException(ErrorCode.SEAT_POSITION_INVALID);
        }
        if (zone == null || !VALID_ZONES.contains(normalizeZone(zone))) {
            throw new SeatException(ErrorCode.SEAT_ZONE_INVALID);
        }
        if (seatType == null || (seatType != 0 && seatType != 1)) {
            throw new SeatException(ErrorCode.SEAT_TYPE_INVALID);
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new SeatException(ErrorCode.SEAT_STATUS_INVALID);
        }
        if ((seatType == 1) != "COUPLE".equals(normalizeZone(zone))) {
            throw new SeatException(ErrorCode.SEAT_LAYOUT_INVALID);
        }
    }

    private void ensureUniquePosition(Long hallId, Long seatId, Integer rowNo, Integer seatNo) {
        LambdaQueryWrapper<Seat> wrapper = new LambdaQueryWrapper<Seat>()
                .eq(Seat::getHallId, hallId)
                .eq(Seat::getRowNo, rowNo)
                .eq(Seat::getSeatNo, seatNo);
        if (seatId != null) {
            wrapper.ne(Seat::getId, seatId);
        }
        if (seatMapper.selectCount(wrapper) > 0) {
            throw new SeatException(ErrorCode.SEAT_POSITION_DUPLICATE);
        }
    }

    private void assertSeatCanChange(Long seatId) {
        List<ShowtimeSeat> inventories = showtimeSeatMapper.selectList(new LambdaQueryWrapper<ShowtimeSeat>()
                .eq(ShowtimeSeat::getSeatId, seatId));
        if (inventories.stream().anyMatch(item -> item.getStatus() != null
                && (item.getStatus() == 1 || item.getStatus() == 2))) {
            throw new SeatException(ErrorCode.SEAT_HAS_ACTIVE_INVENTORY);
        }
        List<Long> inventoryIds = inventories.stream().map(ShowtimeSeat::getId).toList();
        if (!inventoryIds.isEmpty() && orderItemMapper.selectCount(new LambdaQueryWrapper<com.szml.movieticket.entity.OrderItem>()
                .in(com.szml.movieticket.entity.OrderItem::getSeatId, inventoryIds)) > 0) {
            throw new SeatException(ErrorCode.SEAT_HAS_ORDER_RECORD);
        }
    }

    private void syncSeatToShowtimes(Seat seat) {
        List<Showtime> showtimes = showtimeMapper.selectList(new LambdaQueryWrapper<Showtime>()
                .eq(Showtime::getHallId, seat.getHallId()));
        int targetStatus = inventoryStatus(seat);
        for (Showtime showtime : showtimes) {
            ShowtimeSeat inventory = showtimeSeatMapper.selectOne(new LambdaQueryWrapper<ShowtimeSeat>()
                    .eq(ShowtimeSeat::getShowtimeId, showtime.getId())
                    .eq(ShowtimeSeat::getSeatId, seat.getId()));
            if (inventory == null) {
                inventory = new ShowtimeSeat();
                inventory.setShowtimeId(showtime.getId());
                inventory.setSeatId(seat.getId());
                inventory.setPrice(showtime.getBasePrice());
                inventory.setStatus(targetStatus);
                inventory.setVersion(0);
                showtimeSeatMapper.insert(inventory);
            } else if (inventory.getStatus() == null || inventory.getStatus() == 0
                    || inventory.getStatus() == 3 || inventory.getStatus() == 4) {
                inventory.setStatus(targetStatus);
                showtimeSeatMapper.updateById(inventory);
            }
        }
    }

    private static int inventoryStatus(Seat seat) {
        if (seat.getStatus() != null && seat.getStatus() == 1) {
            return 3;
        }
        return seat.getSeatType() != null && seat.getSeatType() == 1 ? 4 : 0;
    }

    private static boolean seatChanged(Seat seat, SeatLayoutItemDTO item) {
        int status = item.getStatus() == null ? 0 : item.getStatus();
        return !Objects.equals(seat.getRowNo(), item.getRowNo())
                || !Objects.equals(seat.getSeatNo(), item.getSeatNo())
                || !Objects.equals(seat.getZone(), normalizeZone(item.getZone()))
                || !Objects.equals(seat.getSeatType(), item.getSeatType())
                || !Objects.equals(seat.getStatus(), status);
    }

    private static String positionKey(Integer rowNo, Integer seatNo) {
        return rowNo + ":" + seatNo;
    }

    private static String normalizeZone(String zone) {
        return zone == null ? null : zone.trim().toUpperCase(Locale.ROOT);
    }

    private static SeatVO toSeatVO(Seat seat) {
        SeatVO vo = new SeatVO();
        vo.setId(seat.getId());
        vo.setHallId(seat.getHallId());
        vo.setRowNo(seat.getRowNo());
        vo.setSeatNo(seat.getSeatNo());
        vo.setZone(seat.getZone());
        vo.setSeatType(seat.getSeatType());
        vo.setSeatTypeDesc(seat.getSeatType() != null && seat.getSeatType() == 1 ? "情侣座" : "普通座");
        vo.setStatus(seat.getStatus());
        vo.setStatusDesc(seat.getStatus() != null && seat.getStatus() == 1 ? "不可用" : "可用");
        return vo;
    }
}
