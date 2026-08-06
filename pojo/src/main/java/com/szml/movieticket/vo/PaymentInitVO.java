package com.szml.movieticket.vo;

import lombok.Data;

/**
 * 支付初始化结果。payForm 用于 WAP 收银台，qrCode 用于二维码支付。
 */
@Data
public class PaymentInitVO {

    private Long orderId;

    private String outTradeNo;

    private String paymentStatus;

    private String payForm;

    private String qrCode;
}
