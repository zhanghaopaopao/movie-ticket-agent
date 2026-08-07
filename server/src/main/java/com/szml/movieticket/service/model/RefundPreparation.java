package com.szml.movieticket.service.model;

import com.szml.movieticket.entity.PaymentRefund;

/** 退款准备阶段返回的上下文。 */
public record RefundPreparation(PaymentRefund refund, boolean shouldSubmit) {
}
