package com.szml.movieticket.service;

import com.szml.movieticket.service.model.AlipayRefundResult;

import java.util.Map;

/**
 * Alipay WAP gateway service.
 *
 * @author zhanghao
 * @since 2026-08-04
 */
public interface AlipayPaymentService {

    /** Creates the HTML form used to redirect a customer to Alipay. */
    String createWapPayForm(String outTradeNo, String subject, Integer amountFen);

    /** Creates the QR-code content used by the face-to-face sandbox payment flow. */
//    String createPrecreateQrCode(String outTradeNo, String subject, Integer amountFen);

    /** Verifies the signature and application identity of an Alipay notification. */
    boolean verifyNotification(Map<String, String> params);

    /** 发起支付宝整单退款。 */
    AlipayRefundResult refund(String outTradeNo, String tradeNo, String outRequestNo, Integer amountFen);

    /** 查询支付宝退款结果。 */
    AlipayRefundResult queryRefund(String outTradeNo, String tradeNo, String outRequestNo, Integer amountFen);
}
