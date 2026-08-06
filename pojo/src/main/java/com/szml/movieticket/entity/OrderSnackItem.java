package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 订单中的零食快照。 */
@Data
@TableName("order_snack_item")
public class OrderSnackItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long snackId;

    private String snackName;

    /** 下单时的单价快照，单位为分。 */
    private Integer unitPriceFen;

    private Integer quantity;

    /** 库存状态：RESERVED、SOLD 或 RELEASED。 */
    private String inventoryStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
