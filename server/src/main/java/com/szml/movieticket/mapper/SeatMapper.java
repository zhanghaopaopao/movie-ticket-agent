package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.Seat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 座位 Mapper。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Mapper
public interface SeatMapper extends BaseMapper<Seat> {
}
