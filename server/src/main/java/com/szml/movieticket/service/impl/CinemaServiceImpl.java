package com.szml.movieticket.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.dto.CinemaCreateDTO;
import com.szml.movieticket.dto.CinemaStatusDTO;
import com.szml.movieticket.dto.CinemaUpdateDTO;
import com.szml.movieticket.entity.Cinema;
import com.szml.movieticket.entity.Hall;
import com.szml.movieticket.entity.Showtime;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.enums.CinemaStatus;
import com.szml.movieticket.enums.ShowtimeStatus;
import com.szml.movieticket.exception.CinemaException;
import com.szml.movieticket.mapper.CinemaMapper;
import com.szml.movieticket.mapper.HallMapper;
import com.szml.movieticket.mapper.ShowtimeMapper;
import com.szml.movieticket.service.CinemaService;
import com.szml.movieticket.vo.CinemaPageVO;
import com.szml.movieticket.vo.CinemaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 影院服务实现类。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CinemaServiceImpl extends ServiceImpl<CinemaMapper, Cinema> implements CinemaService {

    private final HallMapper hallMapper;
    private final ShowtimeMapper showtimeMapper;

    @Override
    public CinemaPageVO pageCinemas(int page, int size, String keyword, String district, Integer status) {
        LambdaQueryWrapper<Cinema> wrapper = buildQueryWrapper(keyword, district, status);//根据相应条件构建查询条件
        wrapper.orderByDesc(Cinema::getCreateTime);

        Page<Cinema> pageResult = page(new Page<>(page, size), wrapper);
        List<CinemaVO> records = pageResult.getRecords().stream().map(this::toVO).collect(Collectors.toList());

        CinemaPageVO pageVO = new CinemaPageVO();
        pageVO.setTotal(pageResult.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setRecords(records);
        return pageVO;
    }

    @Override
    public CinemaVO getCinemaDetail(Long id) {
        Cinema cinema = getById(id);
        if (cinema == null) {
            throw new CinemaException(ErrorCode.CINEMA_NOT_FOUND);
        }
        return toVO(cinema);
    }

    @Override
    public CinemaVO createCinema(CinemaCreateDTO dto) {
        long count = count(new LambdaQueryWrapper<Cinema>().eq(Cinema::getName, dto.getName()));
        if (count > 0) {
            throw new CinemaException(ErrorCode.CINEMA_NAME_DUPLICATE);
        }

        Cinema cinema = new Cinema();
        BeanUtils.copyProperties(dto, cinema);
        if (dto.getServices() != null) {
            cinema.setServices(JSONUtil.toJsonStr(dto.getServices()));
        }
        cinema.setStatus(CinemaStatus.ACTIVE);
        save(cinema);

        log.info("影院新增成功, id: {}, name: {}", cinema.getId(), cinema.getName());
        return toVO(cinema);
    }

    @Override
    public CinemaVO updateCinema(Long id, CinemaUpdateDTO dto) {
        Cinema cinema = getById(id);
        if (cinema == null) {
            throw new CinemaException(ErrorCode.CINEMA_NOT_FOUND);
        }

        if (StringUtils.hasText(dto.getName()) && !dto.getName().equals(cinema.getName())) {
            long count = count(new LambdaQueryWrapper<Cinema>().eq(Cinema::getName, dto.getName()));
            if (count > 0) {
                throw new CinemaException(ErrorCode.CINEMA_NAME_DUPLICATE);
            }
        }

        List<String> updatedFields = new ArrayList<>();
        if (StringUtils.hasText(dto.getName())) { cinema.setName(dto.getName()); updatedFields.add("name"); }
        if (StringUtils.hasText(dto.getAddress())) { cinema.setAddress(dto.getAddress()); updatedFields.add("address"); }
        if (StringUtils.hasText(dto.getDistrict())) { cinema.setDistrict(dto.getDistrict()); updatedFields.add("district"); }
        if (dto.getBrand() != null) { cinema.setBrand(dto.getBrand()); updatedFields.add("brand"); }
        if (dto.getLatitude() != null) { cinema.setLatitude(dto.getLatitude()); updatedFields.add("latitude"); }
        if (dto.getLongitude() != null) { cinema.setLongitude(dto.getLongitude()); updatedFields.add("longitude"); }
        if (dto.getServices() != null) { cinema.setServices(JSONUtil.toJsonStr(dto.getServices())); updatedFields.add("services"); }

        updateById(cinema);

        log.info("影院编辑成功, id: {}, updatedFields: {}", id, updatedFields);
        return toVO(cinema);
    }

    @Override
    public CinemaVO updateCinemaStatus(Long id, CinemaStatusDTO dto) {
        Cinema cinema = getById(id);
        if (cinema == null) {
            throw new CinemaException(ErrorCode.CINEMA_NOT_FOUND);
        }

        if (dto.getStatus() == CinemaStatus.INACTIVE) {
            List<Hall> halls = hallMapper.selectList(new LambdaQueryWrapper<Hall>().eq(Hall::getCinemaId, id));
            if (!halls.isEmpty()) {
                List<Long> hallIds = halls.stream().map(Hall::getId).toList();
                long activeCount = showtimeMapper.selectCount(
                        new LambdaQueryWrapper<Showtime>()
                                .in(Showtime::getHallId, hallIds)
                                .eq(Showtime::getStatus, ShowtimeStatus.ON_SALE));
                if (activeCount > 0) {
                    throw new CinemaException(ErrorCode.CINEMA_HAS_ACTIVE_SHOWTIMES);
                }
            }
        }

        cinema.setStatus(dto.getStatus());
        updateById(cinema);

        log.info("影院状态变更, id: {}, newStatus: {}", id, dto.getStatus());
        return toVO(cinema);
    }

    @Override
    public CinemaPageVO listCinemasForUser(int page, int size, String district, String brand, String hallType, String keyword) {
        LambdaQueryWrapper<Cinema> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cinema::getStatus, CinemaStatus.ACTIVE);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Cinema::getName, keyword);
        }
        if (StringUtils.hasText(district)) {
            wrapper.eq(Cinema::getDistrict, district);
        }
        if (StringUtils.hasText(brand)) {
            wrapper.eq(Cinema::getBrand, brand);
        }
        if (StringUtils.hasText(hallType)) {
            // 查有该厅型的影院
            List<Hall> halls = hallMapper.selectList(
                    new LambdaQueryWrapper<Hall>().eq(Hall::getHallType, hallType));
            Set<Long> cinemaIds = halls.stream().map(Hall::getCinemaId).collect(Collectors.toSet());
            if (cinemaIds.isEmpty()) {
                CinemaPageVO empty = new CinemaPageVO();
                empty.setTotal(0); empty.setPage(page); empty.setSize(size); empty.setRecords(new ArrayList<>());
                return empty;
            }
            wrapper.in(Cinema::getId, cinemaIds);
        }
        wrapper.orderByDesc(Cinema::getCreateTime);

        Page<Cinema> pageResult = page(new Page<>(page, size), wrapper);
        List<CinemaVO> records = pageResult.getRecords().stream().map(cinema -> {
            CinemaVO vo = toVO(cinema);
            vo.setMinPrice(getMinPrice(cinema.getId()));
            return vo;
        }).collect(Collectors.toList());

        CinemaPageVO pageVO = new CinemaPageVO();
        pageVO.setTotal(pageResult.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setRecords(records);
        return pageVO;
    }

    @Override
    public CinemaPageVO listNearbyCinemas(int page, int size, double lat, double lng, int radius) {
        // 矩形预筛选（1°≈111km）
        double latRange = radius / 111.0;
        double lngRange = radius / (111.0 * Math.cos(Math.toRadians(lat)));

        LambdaQueryWrapper<Cinema> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cinema::getStatus, CinemaStatus.ACTIVE)
                .isNotNull(Cinema::getLatitude)
                .isNotNull(Cinema::getLongitude)
                .between(Cinema::getLatitude, lat - latRange, lat + latRange)
                .between(Cinema::getLongitude, lng - lngRange, lng + lngRange);

        List<Cinema> cinemas = list(wrapper);

        // Java 层 Haversine 精算距离并排序
        List<Cinema> sorted = cinemas.stream()
                .filter(c -> {
                    double distance = haversine(lat, lng,
                            c.getLatitude().doubleValue(), c.getLongitude().doubleValue());
                    return distance <= radius;
                })
                .sorted(Comparator.comparingDouble(c ->
                        haversine(lat, lng,
                                c.getLatitude().doubleValue(), c.getLongitude().doubleValue())))
                .toList();

        // 分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, sorted.size());
        List<Cinema> pageList = start < sorted.size() ? sorted.subList(start, end) : new ArrayList<>();

        List<CinemaVO> records = pageList.stream().map(cinema -> {
            CinemaVO vo = toVO(cinema);
            vo.setDistance(haversine(lat, lng, cinema.getLatitude().doubleValue(), cinema.getLongitude().doubleValue()));
            vo.setMinPrice(getMinPrice(cinema.getId()));
            return vo;
        }).collect(Collectors.toList());

        CinemaPageVO pageVO = new CinemaPageVO();
        pageVO.setTotal(sorted.size());
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setRecords(records);
        return pageVO;
    }

    private LambdaQueryWrapper<Cinema> buildQueryWrapper(String keyword, String district, Integer status) {
        LambdaQueryWrapper<Cinema> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Cinema::getName, keyword);
        }
        if (StringUtils.hasText(district)) {
            wrapper.eq(Cinema::getDistrict, district);
        }
        if (status != null) {
            wrapper.eq(Cinema::getStatus, CinemaStatus.fromCode(status));
        }
        return wrapper;
    }

    private CinemaVO toVO(Cinema cinema) {
        CinemaVO vo = new CinemaVO();
        BeanUtils.copyProperties(cinema, vo);
        vo.setStatus(cinema.getStatus() != null ? cinema.getStatus().getCode() : null);
        vo.setStatusDesc(cinema.getStatus() != null ? cinema.getStatus().getDesc() : null);
        vo.setServices(JSONUtil.toList(cinema.getServices(), String.class));

        // 一次查询影厅，同时计算影厅数量、厅型列表、在售场次数
        List<Hall> halls = hallMapper.selectList(
                new LambdaQueryWrapper<Hall>().eq(Hall::getCinemaId, cinema.getId()));
        vo.setHallCount(halls.size());
        vo.setHallTypes(halls.stream()
                .map(h -> h.getHallType() != null ? h.getHallType().getCode() : null)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));//查询影院的厅型列表

        if (!halls.isEmpty()) {
            List<Long> hallIds = halls.stream().map(Hall::getId).toList();
            long showtimeCount = showtimeMapper.selectCount(
                    new LambdaQueryWrapper<Showtime>()
                            .in(Showtime::getHallId, hallIds)
                            .eq(Showtime::getStatus, ShowtimeStatus.ON_SALE));//查询在售场次逻辑
            vo.setShowtimeCount((int) showtimeCount);
        } else {
            vo.setShowtimeCount(0);
        }

        return vo;
    }

    private Double getMinPrice(Long cinemaId) {
        List<Hall> halls = hallMapper.selectList(
                new LambdaQueryWrapper<Hall>().eq(Hall::getCinemaId, cinemaId));
        if (halls.isEmpty()) return null;
        List<Long> hallIds = halls.stream().map(Hall::getId).toList();
        List<Showtime> showtimes = showtimeMapper.selectList(
                new LambdaQueryWrapper<Showtime>()
                        .in(Showtime::getHallId, hallIds)
                        .eq(Showtime::getStatus, ShowtimeStatus.ON_SALE)
                        .orderByAsc(Showtime::getBasePrice));
        if (showtimes.isEmpty()) return null;
        return showtimes.get(0).getBasePrice() != null
                ? showtimes.get(0).getBasePrice() / 100.0 : null;
    }

    /**
     * Haversine 公式计算两点间距离（km）。
     */
    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
