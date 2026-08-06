package com.szml.movieticket.service.impl;

import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
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
import java.util.Map;

/** Alipay SDK adapter for WAP payment and notification verification. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayPaymentServiceImpl implements AlipayPaymentService {

    private final AlipayProperties properties;
    private final ObjectMapper objectMapper;

    private volatile AlipayClient client;

    @Override
    public String createWapPayForm(String outTradeNo, String subject, Integer amountFen) {
        ensureConfigured();
        AlipayTradeWapPayRequest request = new AlipayTradeWapPayRequest();
        request.setReturnUrl(properties.getReturnUrl());
        request.setNotifyUrl(properties.getNotifyUrl());

        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", BigDecimal.valueOf(amountFen, 2)
                .setScale(2, RoundingMode.HALF_UP).toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("product_code", "QUICK_WAP_WAY");
        bizContent.put("quit_url", properties.getReturnUrl());
        try {
            request.setBizContent(objectMapper.writeValueAsString(bizContent));
            // WAP checkout must be submitted as the SDK-generated HTML form so the
            // sandbox can preserve the signed request through its login redirect.
            AlipayTradeWapPayResponse response = getClient().pageExecute(request);
            if (response == null || response.getBody() == null || response.getBody().isBlank()) {
                throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
            }
            return response.getBody();
        } catch (BusinessException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Failed to build Alipay payment request, outTradeNo={}", outTradeNo, e);
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        } catch (Exception e) {
            log.error("Failed to create Alipay WAP payment, outTradeNo={}", outTradeNo, e);
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }
    }

    @Override
    public String createPrecreateQrCode(String outTradeNo, String subject, Integer amountFen) {
        ensureConfigured();
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        request.setNotifyUrl(properties.getNotifyUrl());

        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", BigDecimal.valueOf(amountFen, 2)
                .setScale(2, RoundingMode.HALF_UP).toPlainString());
        bizContent.put("subject", subject);
        bizContent.put("product_code", "FACE_TO_FACE_PAYMENT");
        try {
            request.setBizContent(objectMapper.writeValueAsString(bizContent));
            AlipayTradePrecreateResponse response = getClient().execute(request);
            if (response == null || !response.isSuccess()
                    || response.getQrCode() == null || response.getQrCode().isBlank()) {
                log.error(
                        "Alipay precreate did not return a QR code, outTradeNo={}, subCode={}, subMsg={}",
                        outTradeNo,
                        response == null ? null : response.getSubCode(),
                        response == null ? null : response.getSubMsg());
                throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
            }
            return response.getQrCode();
        } catch (BusinessException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Failed to build Alipay precreate request, outTradeNo={}", outTradeNo, e);
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        } catch (Exception e) {
            log.error("Failed to create Alipay precreate payment, outTradeNo={}", outTradeNo, e);
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
        }
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
