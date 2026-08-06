package com.szml.movieticket.scheduler;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.szml.movieticket.entity.*;
import com.szml.movieticket.mapper.*;
import com.szml.movieticket.service.OrderSnackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 过期订单定时释放调度器 + 惰性过期补充。
 * 每分钟扫描超时的待支付订单，释放座位并标记订单为已过期。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpireScheduler {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ShowtimeSeatMapper showtimeSeatMapper;
    private final SeatLockLogMapper seatLockLogMapper;
    private final PurchaseDraftMapper draftMapper;
    private final PaymentMapper paymentMapper;
    private final OrderSnackService orderSnackService;

    /**
     * 定时任务：每分钟扫描超时的待支付订单，释放座位 + 标记过期。
     * 同时解冻对应的草稿。
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void releaseExpiredOrders() {
        List<TicketOrder> expiredOrders = orderMapper.selectList(
                new LambdaQueryWrapper<TicketOrder>()
                        .eq(TicketOrder::getStatus, "PAYMENT_PENDING")
                        .lt(TicketOrder::getExpiresAt, LocalDateTime.now()));

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("发现超时订单 {} 笔，开始释放座位", expiredOrders.size());

        for (TicketOrder order : expiredOrders) {
            // 释放座位
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            for (OrderItem item : items) {
                ShowtimeSeat seat = showtimeSeatMapper.selectById(item.getSeatId());
                if (seat != null && seat.getStatus() == 1) {
                    seat.setStatus(0);
                    seat.setLockOwner(null);
                    seat.setLockExpiresAt(null);
                    showtimeSeatMapper.updateById(seat);
                }
                // 写释放审计日志
                SeatLockLog log1 = new SeatLockLog();
                log1.setOrderId(order.getId());
                log1.setShowtimeId(order.getShowtimeId());
                log1.setSeatId(item.getSeatId());
                log1.setAction("EXPIRE");
                seatLockLogMapper.insert(log1);
            }

            // 标记订单为已过期
            order.setStatus("EXPIRED");
            orderMapper.updateById(order);

            // 关闭 PENDING 状态的支付记录
            Payment payment = paymentMapper.selectOne(
                    new LambdaQueryWrapper<Payment>()
                            .eq(Payment::getOrderId, order.getId())
                            .orderByDesc(Payment::getId)
                            .last("LIMIT 1"));
            if (payment != null && "PENDING".equals(payment.getStatus())) {
                payment.setStatus("CLOSED");
                paymentMapper.updateById(payment);
            }

            // 订单过期时释放预占的零食库存，零食明细保留为 RELEASED。
            orderSnackService.releaseReserved(order.getId());

            // 解冻草稿
            PurchaseDraft draft = draftMapper.selectOne(
                    new LambdaQueryWrapper<PurchaseDraft>().eq(PurchaseDraft::getOrderId, order.getId()));
            if (draft != null) {
                draft.setStatus("ACTIVE");
                draft.setOrderId(null);
                draft.setVersion(draft.getVersion() + 1);
                draftMapper.updateById(draft);
            }

            log.debug("订单已过期释放, orderId: {}, orderNo: {}", order.getId(), order.getOrderNo());
        }

        log.info("超时订单释放完成, 共处理 {} 笔", expiredOrders.size());
    }
}
