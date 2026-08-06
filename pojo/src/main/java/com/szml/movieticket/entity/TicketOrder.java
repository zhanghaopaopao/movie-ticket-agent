package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体，映射 {@code orders} 表。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
@TableName("orders")
public class TicketOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 场次ID */
    private Long showtimeId;

    /** 订单金额（分） */
    private Integer amount;

    /* 状态：PAYMENT_PENDING/PAID/TICKETED/CANCELLED/EXPIRED */
    /**  case "PAYMENT_PENDING" -> "待支付";
     case "PAID" -> "已支付";
     case "TICKETED" -> "已出票";
     case "CANCELLED" -> "已取消";
     case "EXPIRED" -> "已过期"; */
    private String status;

    /** 支付截止时间 */
    private LocalDateTime expiresAt;

    /** 出票重试次数 */
    private Integer retryCount;

    /** 最近一次出票重试时间 */
    private LocalDateTime lastRetryAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
