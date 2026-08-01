package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细 Mapper。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
