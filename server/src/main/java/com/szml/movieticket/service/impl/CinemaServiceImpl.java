package com.szml.movieticket.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.dto.CinemaCreateDTO;
import com.szml.movieticket.dto.CinemaStatusDTO;
import com.szml.movieticket.dto.CinemaUpdateDTO;
import com.szml.movieticket.entity.Cinema;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.enums.CinemaStatus;
import com.szml.movieticket.exception.CinemaException;
import com.szml.movieticket.mapper.CinemaMapper;
import com.szml.movieticket.service.CinemaService;
import com.szml.movieticket.vo.CinemaPageVO;
import com.szml.movieticket.vo.CinemaVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 影院服务实现类。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@Service
public class CinemaServiceImpl extends ServiceImpl<CinemaMapper, Cinema> implements CinemaService {

    @Override
    public CinemaPageVO pageCinemas(int page, int size, String keyword, String district, Integer status) {
        LambdaQueryWrapper<Cinema> wrapper = buildQueryWrapper(keyword, district, status);
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
            // TODO: 接入 showtime 表后查询 count WHERE cinema_id = id AND status = ON_SALE
            log.warn("影院停用操作，暂未校验在售场次, cinemaId: {}", id);
        }

        cinema.setStatus(dto.getStatus());
        updateById(cinema);

        log.info("影院状态变更, id: {}, newStatus: {}", id, dto.getStatus());
        return toVO(cinema);
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
        vo.setHallCount(0);
        vo.setShowtimeCount(0);
        return vo;
    }
}
