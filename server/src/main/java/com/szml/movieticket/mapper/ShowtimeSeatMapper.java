package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.ShowtimeSeat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 场次座位库存 Mapper。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Mapper
public interface ShowtimeSeatMapper extends BaseMapper<ShowtimeSeat> {
}
