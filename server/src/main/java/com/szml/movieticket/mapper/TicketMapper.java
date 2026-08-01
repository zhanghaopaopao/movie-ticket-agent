package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电子票 Mapper。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
}
