package com.szml.movieticket.vo;

import lombok.Data;

/**
 * 支付初始化结果。payForm 是支付宝 WAP 收银台表单，前端提交后离开当前站点。
 */
@Data
public class PaymentInitVO {

    private Long orderId;

    private String outTradeNo;

    private String paymentStatus;

    private String payForm;
}
