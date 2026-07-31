package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.Showtime;
import org.apache.ibatis.annotations.Mapper;

/**
 * 场次 Mapper。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Mapper
public interface ShowtimeMapper extends BaseMapper<Showtime> {
}
