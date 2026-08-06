package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.szml.movieticket.config.AlipayProperties;
import com.szml.movieticket.entity.Payment;
import com.szml.movieticket.mapper.PaymentMapper;
import com.szml.movieticket.service.AlipayCallbackService;
import com.szml.movieticket.service.AlipayPaymentService;
import com.szml.movieticket.service.OrderTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/** Coordinates Alipay callbacks with order payment transactions. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayCallbackServiceImpl implements AlipayCallbackService {

    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";

    private final AlipayPaymentService alipayPaymentService;
    private final OrderTicketService orderTicketService;
    private final PaymentMapper paymentMapper;
    private final AlipayProperties properties;

    @Override
    public String handleNotification(Map<String, String> params) {
        if (!alipayPaymentService.verifyNotification(params)) {
            log.warn("Alipay notification signature verification failed");
            return FAILURE;
        }

        try {
            String tradeStatus = params.get("trade_status");
            if ("TRADE_CLOSED".equals(tradeStatus)) {
                orderTicketService.handleAlipayClosed(
                        params.get("out_trade_no"), params.get("notify_time"));
                return SUCCESS;
            }
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                return SUCCESS;
            }

            BigDecimal totalAmount = new BigDecimal(params.get("total_amount"));
            orderTicketService.handleAlipaySuccess(
                    params.get("out_trade_no"),
                    params.get("trade_no"),
                    totalAmount,
                    params.get("notify_time"));
            return SUCCESS;
        } catch (RuntimeException e) {
            log.error("Failed to process Alipay notification", e);
            return FAILURE;
        }
    }

    @Override
    public String buildFrontendReturnUrl(String outTradeNo) {
        return buildFrontendReturnUrl(outTradeNo, false);
    }

    @Override
    public String buildFrontendReturnUrl(String outTradeNo, boolean cancelled) {
        Long orderId = findOrderId(outTradeNo);
        String target = cancelled
                ? properties.getFrontendPayUrl()
                : properties.getFrontendReturnUrl();
        if (target == null || target.isBlank()) {
            target = cancelled
                    ? "http://localhost:8001/orders/{orderId}/pay?alipayCancelled=1"
                    : "http://localhost:8001/orders/{orderId}/pay/result";
        }
        if (orderId == null) {
            if (cancelled) {
                return "http://localhost:8001/me/orders";
            }
            return target.replace("/{orderId}", "").replace("{orderId}", "");
        }
        return target.replace("{orderId}", String.valueOf(orderId));
    }

    private Long findOrderId(String outTradeNo) {
        if (outTradeNo == null || outTradeNo.isBlank()) {
            return null;
        }
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOutTradeNo, outTradeNo)
                .last("LIMIT 1"));
        return payment != null ? payment.getOrderId() : null;
    }
}
