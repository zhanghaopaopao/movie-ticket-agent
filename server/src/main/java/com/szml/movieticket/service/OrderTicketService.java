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

    /**
     * 模拟支付。
     *
     * @param userId         用户ID
     * @param orderId        订单ID
     * @param idempotencyKey 幂等键
     * @return 支付结果
     */
    PayResultVO pay(Long userId, Long orderId, String idempotencyKey);

    /**
     * 取消订单。
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     */
    void cancelOrder(Long userId, Long orderId);

    /**
     * C 端订单列表。
     */
    UserOrderPageVO listOrders(Long userId, int page, int size, String status);

    /**
     * C 端订单详情。
     */
    UserOrderDetailVO getOrderDetail(Long userId, Long orderId);
}
