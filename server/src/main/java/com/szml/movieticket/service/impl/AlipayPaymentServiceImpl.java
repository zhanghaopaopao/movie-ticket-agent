package com.szml.movieticket.service.impl;

import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.szml.movieticket.config.AlipayProperties;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.BusinessException;
import com.szml.movieticket.service.AlipayPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 支付宝电脑网站、WAP 支付及通知验签适配器。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayPaymentServiceImpl implements AlipayPaymentService {

    private final AlipayProperties properties;
    private final ObjectMapper objectMapper;

    private volatile AlipayClient client;

    @Override
    public String createPayForm(String outTradeNo, String subject, Integer amountFen,
                                Long orderId, String userAgent) {
        ensureConfigured();
        try {
            if (isMobileClient(userAgent)) {
                return createWapPayForm(outTradeNo, subject, amountFen, orderId);
            }
            return createPagePayForm(outTradeNo, subject, amountFen);
        } catch (BusinessException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("生成支付宝支付参数失败, outTradeNo={}", outTradeNo, e);
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        } catch (Exception e) {
            log.error("创建支付宝支付失败, outTradeNo={}", outTradeNo, e);
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }
    }

    /** 创建手机浏览器使用的 WAP 支付表单。 */
    private String createWapPayForm(String outTradeNo, String subject, Integer amountFen,
                                    Long orderId) throws Exception {
        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setReturnUrl(properties.getReturnUrl());
        request.setNotifyUrl(properties.getNotifyUrl());

        Map<String, Object> bizContent = buildCommonBizContent(outTradeNo, subject, amountFen);
        bizContent.put("product_code", "QUICK_WAP_WAY");
        bizContent.put("quit_url", buildQuitUrl(orderId));
        request.setBizContent(objectMapper.writeValueAsString(bizContent));

        AlipayTradeWapPayResponse response = getClient().pageExecute(request);
        return requireFormBody(response != null ? response.getBody() : null);
    }

    /** 创建桌面浏览器使用的电脑网站支付表单。 */
    private String createPagePayForm(String outTradeNo, String subject, Integer amountFen)
            throws Exception {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setReturnUrl(properties.getReturnUrl());
        request.setNotifyUrl(properties.getNotifyUrl());

        Map<String, Object> bizContent = buildCommonBizContent(outTradeNo, subject, amountFen);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        bizContent.put("qr_pay_mode", 2);
        request.setBizContent(objectMapper.writeValueAsString(bizContent));

        AlipayTradePagePayResponse response = getClient().pageExecute(request);
        return requireFormBody(response != null ? response.getBody() : null);
    }

    private Map<String, Object> buildCommonBizContent(String outTradeNo, String subject,
                                                       Integer amountFen) {
        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", BigDecimal.valueOf(amountFen, 2)
                .setScale(2, RoundingMode.HALF_UP).toPlainString());
        bizContent.put("subject", subject);
        return bizContent;
    }

    private String requireFormBody(String body) {
        if (body == null || body.isBlank()) {
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }
        return body;
    }

    /** 根据浏览器标识选择支付通道，无法识别时默认使用桌面网站支付。 */
    static boolean isMobileClient(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return false;
        }
        String value = userAgent.toLowerCase(Locale.ROOT);
        return value.contains("mobile")
                || value.contains("android")
                || value.contains("iphone")
                || value.contains("ipad")
                || value.contains("ipod")
                || value.contains("windows phone")
                || value.contains("harmonyos");
    }

    /** 支付退出地址直接返回原订单支付页，不经过支付成功回调。 */
    private String buildQuitUrl(Long orderId) {
        String template = properties.getFrontendPayUrl();
        if (template == null || template.isBlank()) {
            template = "http://localhost:8001/orders/{orderId}/pay?alipayCancelled=1";
        }
        return template.replace("{orderId}", String.valueOf(orderId));
    }

    @Override
    public boolean verifyNotification(Map<String, String> params) {
        if (!properties.isConfigured() || params == null || params.isEmpty()) {
            return false;
        }
        try {
            if (!properties.getAppId().equals(params.get("app_id"))) {
                return false;
            }
            return AlipaySignature.rsaCheckV1(
                    params, properties.getAlipayPublicKey(), "UTF-8", "RSA2");
        } catch (Exception e) {
            log.warn("Failed to verify Alipay notification", e);
            return false;
        }
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_CONFIGURED);
        }
    }

    private AlipayClient getClient() {
        AlipayClient current = client;
        if (current == null) {
            synchronized (this) {
                current = client;
                if (current == null) {
                    ensureConfigured();
                    AlipayConfig config = new AlipayConfig();
                    config.setServerUrl(properties.getGatewayUrl());
                    config.setAppId(properties.getAppId());
                    config.setPrivateKey(properties.getMerchantPrivateKey());
                    config.setFormat("json");
                    config.setCharset("UTF-8");
                    config.setAlipayPublicKey(properties.getAlipayPublicKey());
                    config.setSignType("RSA2");
                    try {
                        current = new DefaultAlipayClient(config);
                        client = current;
                    } catch (Exception e) {
                        log.error("Failed to initialize Alipay client", e);
                        throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
                    }
                }
            }
        }
        return current;
    }
}
