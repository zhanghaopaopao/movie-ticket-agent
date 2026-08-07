package com.szml.movieticket.service.model;

import lombok.Getter;

import java.math.BigDecimal;

/** 支付宝退款调用结果，不向 Controller 暴露 SDK 类型。 */
@Getter
public final class AlipayRefundResult {

    public enum Status {
        SUCCESS,
        FAIL,
        PENDING
    }

    private final Status status;
    private final BigDecimal amount;
    private final String code;
    private final String message;

    private AlipayRefundResult(Status status, BigDecimal amount, String code, String message) {
        this.status = status;
        this.amount = amount;
        this.code = code;
        this.message = message;
    }

    public static AlipayRefundResult success(BigDecimal amount, String message) {
        return new AlipayRefundResult(Status.SUCCESS, amount, null, message);
    }

    public static AlipayRefundResult fail(String code, String message) {
        return new AlipayRefundResult(Status.FAIL, null, code, message);
    }

    public static AlipayRefundResult pending(String code, String message) {
        return new AlipayRefundResult(Status.PENDING, null, code, message);
    }
}
