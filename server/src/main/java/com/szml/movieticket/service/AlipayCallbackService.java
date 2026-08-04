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
}
