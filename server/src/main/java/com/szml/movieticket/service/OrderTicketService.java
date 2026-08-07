package com.szml.movieticket.service;

import com.szml.movieticket.vo.*;

/**
 * C 端订单与支付服务接口。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
public interface OrderTicketService {

    /**
     * 锁座。
     *
     * @param userId       用户ID
     * @param showtimeId   场次ID
     * @param seatIds      座位ID列表
     * @param draftVersion 草稿版本号
     * @return 锁座结果
     */
    LockResultVO lockSeats(Long userId, Long showtimeId, java.util.List<Long> seatIds, Integer draftVersion);

    /** 创建支付宝 WAP 支付交易。 */
    PaymentInitVO createPayment(Long userId, Long orderId, String idempotencyKey);

    /** 创建支付宝二维码支付交易。 */
//    PaymentInitVO createQrPayment(Long userId, Long orderId, String idempotencyKey);

    /** 处理支付宝验签后的成功通知。方法必须具备幂等性。 */
    void handleAlipaySuccess(String outTradeNo, String tradeNo, java.math.BigDecimal totalAmount,
                             String notifyTime);

    /** 记录支付宝已关闭交易，不改变订单和座位状态。 */
    void handleAlipayClosed(String outTradeNo, String notifyTime);

    /**
     * 取消订单。
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     */
    void cancelOrder(Long userId, Long orderId);

    /**
     * 退票（已出票订单退款，座位释放）。
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     */

    /**
     * C 端订单列表。
     */
    UserOrderPageVO listOrders(Long userId, int page, int size, String status);

    /**
     * C 端订单详情。
     */
    UserOrderDetailVO getOrderDetail(Long userId, Long orderId);
}
