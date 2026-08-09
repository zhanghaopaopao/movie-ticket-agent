package com.szml.movieticket.service;

import com.szml.movieticket.entity.PaymentRefund;
import com.szml.movieticket.service.model.RefundPreparation;
import com.szml.movieticket.vo.RefundResultVO;

import java.util.List;

/** 退款相关短事务接口。 */
public interface OrderRefundTransactionService {

    /** 校验订单并创建或复用退款尝试。 */
    RefundPreparation prepare(Long userId, Long orderId);

    /** 支付宝确认成功后完成本地座位、零食和电子票结算。 */
    void settleSuccess(Long refundId, Integer actualAmountFen, String providerMessage);

    /** 支付宝明确失败后恢复订单和电子票状态。 */
    void markFailure(Long refundId, String failureCode, String failureMessage);

    /** 记录一次待处理查询，保留退款中的状态。 */
    void recordPendingQuery(Long refundId, String failureCode, String message);

    /** 查询当前用户订单的退款结果。 */
    RefundResultVO getStatus(Long userId, Long orderId);

    /** 获取订单最新一笔待确认退款，用于管理员安全重试。 */
    PaymentRefund getPendingRefund(Long orderId);

    /** 获取待自动对账的退款记录。 */
    List<PaymentRefund> listPending();
}
