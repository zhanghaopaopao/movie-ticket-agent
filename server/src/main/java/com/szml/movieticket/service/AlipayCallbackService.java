package com.szml.movieticket.service;

import java.util.Map;

/**
 * Alipay callback orchestration service.
 *
 * @author zhanghao
 * @since 2026-08-04
 */
public interface AlipayCallbackService {

    /** Handles an asynchronous Alipay notification and returns its protocol response. */
    String handleNotification(Map<String, String> params);

    /** Resolves the C-end result page for an Alipay synchronous return. */
    String buildFrontendReturnUrl(String outTradeNo);

    /** 根据是否主动退出支付，生成当前订单对应的 C 端返回地址。 */
    String buildFrontendReturnUrl(String outTradeNo, boolean cancelled);
}
