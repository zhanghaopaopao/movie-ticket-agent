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

    /** 支付渠道：ALIPAY_SANDBOX */
    private String provider;

    /** 商户订单号，提交给支付宝的 out_trade_no */
    private String outTradeNo;

    /** 支付宝交易号 */
    private String tradeNo;

    /** 支付商品标题 */
    private String subject;

    /** 幂等键 */
    private String idempotencyKey;

    /** 支付状态：PENDING / SUCCESS / FAIL / CLOSED */
    private String status;

    /** 支付金额（分） */
    private Integer amount;

    /** 支付处理时间 */
    private LocalDateTime processedAt;

    /** 支付宝异步通知时间 */
    private LocalDateTime notifyTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
