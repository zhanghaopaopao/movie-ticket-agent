package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电子票实体。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
@TableName("ticket")
public class Ticket {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 订单明细ID */
    private Long orderItemId;

    /** 取票码 */
    private String ticketCode;

    /** 二维码内容 JSON */
    private String qrContent;

    /** 状态：0=有效，1=已使用，2=已退票，3=退款处理中。 */
    private Integer status;

    private LocalDateTime createTime;
}
