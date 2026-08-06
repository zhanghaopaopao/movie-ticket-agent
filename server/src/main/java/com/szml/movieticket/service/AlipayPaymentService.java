package com.szml.movieticket.service;

import java.util.Map;

/**
 * 支付宝支付网关服务。
 *
 * @author zhanghao
 * @since 2026-08-04
 */
public interface AlipayPaymentService {

    /** 根据客户端类型创建电脑网站或 WAP 收银台 HTML 表单。 */
    String createPayForm(String outTradeNo, String subject, Integer amountFen,
                         Long orderId, String userAgent);

    /** Verifies the signature and application identity of an Alipay notification. */
    boolean verifyNotification(Map<String, String> params);
}
