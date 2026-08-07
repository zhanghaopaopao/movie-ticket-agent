package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 支付宝退款尝试记录。 */
@Data
@TableName("payment_refund")
public class PaymentRefund {

    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAIL = "FAIL";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原支付记录 ID。 */
    private Long paymentId;

    /** 订单 ID。 */
    private Long orderId;

    /** 本次退款请求的幂等号。 */
    private String outRequestNo;

    /** 原支付宝商户订单号。 */
    private String outTradeNo;

    /** 原支付宝交易号。 */
    private String tradeNo;

    /** 请求退款金额，单位为分。 */
    private Integer refundAmountFen;

    /** 支付宝实际返回的退款金额，单位为分。 */
    private Integer actualAmountFen;

    /** 退款状态：PENDING、SUCCESS、FAIL。 */
    private String status;

    /** 支付宝错误码。 */
    private String failureCode;

    /** 支付宝或本地处理失败信息。 */
    private String failureMessage;

    /** 已查询支付宝的次数。 */
    private Integer queryCount;

    /** 最近一次查询时间。 */
    private LocalDateTime lastQueryAt;

    /** 退款完成或明确失败时间。 */
    private LocalDateTime processedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
