package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.dto.HallCreateDTO;
import com.szml.movieticket.dto.HallUpdateDTO;
import com.szml.movieticket.entity.Cinema;
import com.szml.movieticket.entity.Hall;
import com.szml.movieticket.entity.Seat;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.HallException;
import com.szml.movieticket.mapper.CinemaMapper;
import com.szml.movieticket.mapper.HallMapper;
import com.szml.movieticket.mapper.SeatMapper;
import com.szml.movieticket.service.HallService;
import com.szml.movieticket.vo.HallSeatVO;
import com.szml.movieticket.vo.HallVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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

    private final CinemaMapper cinemaMapper;
    private final SeatMapper seatMapper;

    @Override
    public List<HallVO> listHallsByCinemaId(Long cinemaId) {
        List<Hall> halls = list(new LambdaQueryWrapper<Hall>()
                .eq(Hall::getCinemaId, cinemaId)
                .orderByAsc(Hall::getId));
        return halls.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public HallVO createHall(HallCreateDTO dto) {
        // 同一影院下名称唯一
        long count = count(new LambdaQueryWrapper<Hall>()
                .eq(Hall::getCinemaId, dto.getCinemaId())
                .eq(Hall::getName, dto.getName()));
        if (count > 0) {
            throw new HallException(ErrorCode.HALL_NAME_DUPLICATE);
        }

        Hall hall = new Hall();
        BeanUtils.copyProperties(dto, hall);
        save(hall);

        log.info("影厅新增成功, id: {}, name: {}, cinemaId: {}", hall.getId(), hall.getName(), hall.getCinemaId());
        return toVO(hall);
    }

    @Override
    public HallVO updateHall(Long id, HallUpdateDTO dto) {
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
        return toVO(hall);
    }

    @Override
    public HallSeatVO getHallSeats(Long hallId) {
        Hall hall = getById(hallId);
        if (hall == null) {
            throw new HallException(ErrorCode.HALL_NOT_FOUND);
        }

        Cinema cinema = cinemaMapper.selectById(hall.getCinemaId());

        List<Seat> seats = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getHallId, hallId)
                .orderByAsc(Seat::getRowNo, Seat::getSeatNo));

        // 统计
        Map<String, Integer> summary = new LinkedHashMap<>();
        summary.put("totalSeats", seats.size());
        summary.put("normalSeats", (int) seats.stream().filter(s -> s.getSeatType() == 0).count());
        summary.put("coupleSeats", (int) seats.stream().filter(s -> s.getSeatType() == 1).count());
        summary.put("unavailableSeats", (int) seats.stream().filter(s -> s.getStatus() == 1).count());

        // 按排分组
        Map<Integer, List<HallSeatVO.SeatItemVO>> rowMap = new LinkedHashMap<>();
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

    private HallVO toVO(Hall hall) {
        HallVO vo = new HallVO();
        BeanUtils.copyProperties(hall, vo);
        vo.setHallType(hall.getHallType() != null ? hall.getHallType().getCode() : null);
        vo.setHallTypeDesc(hall.getHallType() != null ? hall.getHallType().getDesc() : null);
        // 统计座位数
        long seatCount = seatMapper.selectCount(new LambdaQueryWrapper<Seat>().eq(Seat::getHallId, hall.getId()));
        vo.setTotalSeats((int) seatCount);
        return vo;
    }
}
