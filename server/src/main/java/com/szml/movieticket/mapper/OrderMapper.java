package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.TicketOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Mapper
public interface OrderMapper extends BaseMapper<TicketOrder> {
}
