package com.szml.movieticket.service;

import com.szml.movieticket.vo.RefundResultVO;

/** C 端支付宝退款编排接口。 */
public interface OrderRefundService {

    /** 发起或复用一笔整单退款。 */
    RefundResultVO requestRefund(Long userId, Long orderId);

    /** 查询退款状态。 */
    RefundResultVO getRefundStatus(Long userId, Long orderId);

    /** 定时查询支付宝并收敛待处理退款。 */
    void reconcilePendingRefunds();
}
