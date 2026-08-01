package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 订单座位明细实体。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 座位ID（showtime_seat.id） */
    private Long seatId;

    /** 该座位单价（分） */
    private Integer unitPrice;
}
