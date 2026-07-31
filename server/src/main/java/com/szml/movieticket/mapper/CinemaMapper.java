package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.Cinema;
import org.apache.ibatis.annotations.Mapper;

/**
 * 影院 Mapper。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Mapper
public interface CinemaMapper extends BaseMapper<Cinema> {
}
