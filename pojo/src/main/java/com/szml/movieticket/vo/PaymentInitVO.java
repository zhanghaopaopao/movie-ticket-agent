package com.szml.movieticket.vo;

import lombok.Data;

/**
 * 支付初始化结果。payForm 是支付宝电脑网站或 WAP 收银台表单。
 */
@Data
public class PaymentInitVO {

    private Long orderId;

    private String outTradeNo;

    private String paymentStatus;

    private String payForm;
}
