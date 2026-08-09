package com.szml.movieticket.service.impl;

import com.szml.movieticket.entity.PaymentRefund;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.BusinessException;
import com.szml.movieticket.service.AlipayPaymentService;
import com.szml.movieticket.service.OrderRefundService;
import com.szml.movieticket.service.OrderRefundTransactionService;
import com.szml.movieticket.service.model.AlipayRefundResult;
import com.szml.movieticket.service.model.RefundPreparation;
import com.szml.movieticket.util.AmountUtil;
import com.szml.movieticket.vo.RefundResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** 支付宝退款编排实现，网络调用不占用数据库事务。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRefundServiceImpl implements OrderRefundService {

    private final AlipayPaymentService alipayPaymentService;
    private final OrderRefundTransactionService refundTransactionService;

    @Override
    public RefundResultVO requestRefund(Long userId, Long orderId) {
        RefundPreparation preparation = refundTransactionService.prepare(userId, orderId);
        PaymentRefund refund = preparation.refund();
        if (PaymentRefund.SUCCESS.equals(refund.getStatus())) {
            return refundTransactionService.getStatus(userId, orderId);//成功返回
        }

        try {
            AlipayRefundResult providerResult = preparation.shouldSubmit()//分两个分支进行,第一次退款,退款异常情况
                    ? alipayPaymentService.refund(
                    refund.getOutTradeNo(), refund.getTradeNo(), refund.getOutRequestNo(), refund.getRefundAmountFen())
                    : alipayPaymentService.queryRefund(
                    refund.getOutTradeNo(), refund.getTradeNo(), refund.getOutRequestNo(), refund.getRefundAmountFen());
            return applyProviderResult(refund, providerResult, userId, orderId);
        } catch (BusinessException e) {
            // 未配置支付渠道时，本次退款根本没有提交给支付宝，必须立即恢复电子票与订单状态。
            if (e.getCode() == ErrorCode.PAYMENT_NOT_CONFIGURED.getCode()) {
                refundTransactionService.markFailure(refund.getId(), "PAYMENT_NOT_CONFIGURED", e.getMessage());
            }//兜底情况
            throw e;
        }
    }

    @Override
    public RefundResultVO getRefundStatus(Long userId, Long orderId) {
        return refundTransactionService.getStatus(userId, orderId);
    }

    @Override
    public RefundResultVO retryPendingRefund(Long orderId) {
        PaymentRefund refund = refundTransactionService.getPendingRefund(orderId);
        log.info("管理员重试支付宝退款, orderId={}, refundId={}", orderId, refund.getId());
        AlipayRefundResult providerResult = alipayPaymentService.refund(
                refund.getOutTradeNo(), refund.getTradeNo(), refund.getOutRequestNo(), refund.getRefundAmountFen());
        return applyProviderResult(refund, providerResult, null, orderId);
    }

    @Override
    public void reconcilePendingRefunds() {
        for (PaymentRefund refund : refundTransactionService.listPending()) {
            try {
                AlipayRefundResult providerResult = alipayPaymentService.queryRefund(
                        refund.getOutTradeNo(), refund.getTradeNo(), refund.getOutRequestNo(), refund.getRefundAmountFen());
                applyProviderResult(refund, providerResult, null, refund.getOrderId());
            } catch (BusinessException e) {
                // 配置错误或本地业务错误不能把订单误标为退款失败，保留给下一次对账。
                log.warn("退款自动对账暂未完成, refundId={}, message={}", refund.getId(), e.getMessage());
                refundTransactionService.recordPendingQuery(refund.getId(), "RECONCILE_ERROR", e.getMessage());
            } catch (RuntimeException e) {
                log.error("退款自动对账异常, refundId={}", refund.getId(), e);
                refundTransactionService.recordPendingQuery(refund.getId(), "RECONCILE_ERROR", "退款结果待确认");
            }
        }
    }

    private RefundResultVO applyProviderResult(PaymentRefund refund, AlipayRefundResult providerResult,
                                                Long userId, Long orderId) {
        if (providerResult.getStatus() == AlipayRefundResult.Status.SUCCESS) {
            Integer actualAmountFen = toFen(providerResult.getAmount());
            refundTransactionService.settleSuccess(refund.getId(), actualAmountFen, providerResult.getMessage());
        } else if (providerResult.getStatus() == AlipayRefundResult.Status.FAIL) {
            refundTransactionService.markFailure(refund.getId(), providerResult.getCode(), providerResult.getMessage());
        } else {
            refundTransactionService.recordPendingQuery(refund.getId(), providerResult.getCode(), providerResult.getMessage());
        }

        if (userId != null) {
            return refundTransactionService.getStatus(userId, orderId);
        }
        RefundResultVO result = new RefundResultVO();
        result.setOrderId(orderId);
        result.setStatus(providerResult.getStatus().name());
        result.setAmount(AmountUtil.yuan(refund.getRefundAmountFen()));
        result.setOutRequestNo(refund.getOutRequestNo());
        result.setMessage(providerResult.getMessage());
        return result;
    }

    private static Integer toFen(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.movePointRight(2).intValueExact();
    }
}
