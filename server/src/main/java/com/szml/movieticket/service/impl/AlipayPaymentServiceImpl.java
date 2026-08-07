package com.szml.movieticket.service.impl;

import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.szml.movieticket.config.AlipayProperties;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.BusinessException;
import com.szml.movieticket.service.AlipayPaymentService;
import com.szml.movieticket.service.model.AlipayRefundResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        // 主动退出支付时携带当前交易号，后端才能返回对应订单的待支付页。
        bizContent.put("quit_url", buildQuitUrl(outTradeNo));
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

    private String buildQuitUrl(String outTradeNo) {
        String returnUrl = properties.getReturnUrl();
        String separator = returnUrl.contains("?") ? "&" : "?";
        return returnUrl + separator
                + "cancelled=true&out_trade_no="
                + URLEncoder.encode(outTradeNo, StandardCharsets.UTF_8);
    }

//    @Override
//    public String createPrecreateQrCode(String outTradeNo, String subject, Integer amountFen) {
//        ensureConfigured();
//        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
//        request.setNotifyUrl(properties.getNotifyUrl());
//
//        Map<String, Object> bizContent = new LinkedHashMap<>();
//        bizContent.put("out_trade_no", outTradeNo);
//        bizContent.put("total_amount", BigDecimal.valueOf(amountFen, 2)
//                .setScale(2, RoundingMode.HALF_UP).toPlainString());
//        bizContent.put("subject", subject);
//        bizContent.put("product_code", "FACE_TO_FACE_PAYMENT");
//        try {
//            request.setBizContent(objectMapper.writeValueAsString(bizContent));
//            AlipayTradePrecreateResponse response = getClient().execute(request);
//            if (response == null || !response.isSuccess()
//                    || response.getQrCode() == null || response.getQrCode().isBlank()) {
//                log.error(
//                        "Alipay precreate did not return a QR code, outTradeNo={}, subCode={}, subMsg={}",
//                        outTradeNo,
//                        response == null ? null : response.getSubCode(),
//                        response == null ? null : response.getSubMsg());
//                throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
//            }
//            return response.getQrCode();
//        } catch (BusinessException e) {
//            throw e;
//        } catch (JsonProcessingException e) {
//            log.error("Failed to build Alipay precreate request, outTradeNo={}", outTradeNo, e);
//            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
//        } catch (Exception e) {
//            log.error("Failed to create Alipay precreate payment, outTradeNo={}", outTradeNo, e);
//            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR);
//        }
//    }

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

    @Override
    public AlipayRefundResult refund(String outTradeNo, String tradeNo, String outRequestNo, Integer amountFen) {
        ensureConfigured();
        if (amountFen == null || amountFen <= 0) {
            return AlipayRefundResult.fail("INVALID_AMOUNT", "退款金额不合法");
        }

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("trade_no", tradeNo);
        bizContent.put("out_request_no", outRequestNo);
        bizContent.put("refund_amount", BigDecimal.valueOf(amountFen, 2)
                .setScale(2, RoundingMode.HALF_UP).toPlainString());
        bizContent.put("refund_reason", "电影票整单退票");
        try {
            request.setBizContent(objectMapper.writeValueAsString(bizContent));
            AlipayTradeRefundResponse response = getClient().execute(request);
            if (response == null) {
                return AlipayRefundResult.pending("EMPTY_RESPONSE", "支付宝未返回退款结果");
            }
            if (!response.isSuccess()) {
                return AlipayRefundResult.fail(
                        firstNonBlank(response.getSubCode(), response.getCode()),
                        firstNonBlank(response.getSubMsg(), response.getMsg(), "支付宝退款失败"));
            }
            Integer actualAmountFen = toFen(response.getRefundFee());
            if (actualAmountFen == null || !actualAmountFen.equals(amountFen)) {
                log.error("支付宝退款金额不匹配, outRequestNo={}, expected={}, actual={}",
                        outRequestNo, amountFen, response.getRefundFee());
                return AlipayRefundResult.pending("AMOUNT_MISMATCH", "支付宝退款金额待核验");
            }
            return AlipayRefundResult.success(BigDecimal.valueOf(actualAmountFen, 2), "支付宝退款成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 网络超时无法判断支付宝是否已经受理，必须保留待对账状态。
            log.warn("调用支付宝退款结果不确定, outRequestNo={}", outRequestNo, e);
            return AlipayRefundResult.pending("REQUEST_UNKNOWN", "支付宝退款结果待确认");
        }
    }

    @Override
    public AlipayRefundResult queryRefund(String outTradeNo, String tradeNo, String outRequestNo, Integer amountFen) {
        ensureConfigured();
        if (amountFen == null || amountFen <= 0) {
            return AlipayRefundResult.fail("INVALID_AMOUNT", "退款金额不合法");
        }

        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("trade_no", tradeNo);
        bizContent.put("out_request_no", outRequestNo);
        try {
            request.setBizContent(objectMapper.writeValueAsString(bizContent));
            AlipayTradeFastpayRefundQueryResponse response = getClient().execute(request);
            if (response == null) {
                return AlipayRefundResult.pending("EMPTY_RESPONSE", "支付宝未返回退款查询结果");
            }
            if (!response.isSuccess()) {
                // 刚提交的退款在支付宝侧尚未可查询时也会进入这里，不能直接恢复订单。
                return AlipayRefundResult.pending(
                        firstNonBlank(response.getSubCode(), response.getCode()),
                        firstNonBlank(response.getSubMsg(), response.getMsg(), "支付宝退款仍在处理中"));
            }
            if (!"REFUND_SUCCESS".equals(response.getRefundStatus())
                    && !"SUCCESS".equals(response.getRefundStatus())) {
                return AlipayRefundResult.pending("REFUND_PROCESSING", "支付宝退款仍在处理中");
            }
            Integer actualAmountFen = toFen(response.getRefundAmount());
            if (actualAmountFen == null || !actualAmountFen.equals(amountFen)) {
                log.error("支付宝退款查询金额不匹配, outRequestNo={}, expected={}, actual={}",
                        outRequestNo, amountFen, response.getRefundAmount());
                return AlipayRefundResult.pending("AMOUNT_MISMATCH", "支付宝退款金额待核验");
            }
            return AlipayRefundResult.success(BigDecimal.valueOf(actualAmountFen, 2), "支付宝退款成功");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("查询支付宝退款结果失败, outRequestNo={}", outRequestNo, e);
            return AlipayRefundResult.pending("QUERY_UNKNOWN", "支付宝退款结果待确认");
        }
    }

    private static Integer toFen(String yuan) {
        if (yuan == null || yuan.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(yuan).movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
