package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付记录实体。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 幂等键 */
    private String idempotencyKey;

    /** 支付状态：SUCCESS / FAIL */
    private String status;

    /** 支付金额（分） */
    private Integer amount;

    /** 支付处理时间 */
    private LocalDateTime processedAt;
}
