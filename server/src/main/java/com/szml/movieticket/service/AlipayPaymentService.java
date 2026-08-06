package com.szml.movieticket.service;

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
}
