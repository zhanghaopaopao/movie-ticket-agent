package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.szml.movieticket.entity.OrderItem;
import com.szml.movieticket.entity.Payment;
import com.szml.movieticket.entity.PaymentRefund;
import com.szml.movieticket.entity.SeatLockLog;
import com.szml.movieticket.entity.Showtime;
import com.szml.movieticket.entity.ShowtimeSeat;
import com.szml.movieticket.entity.Ticket;
import com.szml.movieticket.entity.TicketOrder;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.enums.ShowtimeStatus;
import com.szml.movieticket.exception.OrderException;
import com.szml.movieticket.mapper.OrderItemMapper;
import com.szml.movieticket.mapper.OrderMapper;
import com.szml.movieticket.mapper.PaymentMapper;
import com.szml.movieticket.mapper.PaymentRefundMapper;
import com.szml.movieticket.mapper.SeatLockLogMapper;
import com.szml.movieticket.mapper.ShowtimeMapper;
import com.szml.movieticket.mapper.ShowtimeSeatMapper;
import com.szml.movieticket.mapper.TicketMapper;
import com.szml.movieticket.service.OrderRefundTransactionService;
import com.szml.movieticket.service.OrderSnackService;
import com.szml.movieticket.service.model.RefundPreparation;
import com.szml.movieticket.util.AmountUtil;
import com.szml.movieticket.vo.RefundResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 退款状态变更和本地库存结算事务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderRefundTransactionServiceImpl implements OrderRefundTransactionService {

    private static final String REDIS_LOCK_PREFIX = "lock:showtime:";
    private static final int TICKET_VALID = 0;
    private static final int TICKET_USED = 1;
    private static final int TICKET_REFUNDED = 2;
    private static final int TICKET_REFUND_PENDING = 3;

    /** 仅删除当前用户自己的锁，防止删除被其他订单重新持有的座位锁。 */
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final PaymentRefundMapper paymentRefundMapper;
    private final TicketMapper ticketMapper;
    private final ShowtimeMapper showtimeMapper;
    private final ShowtimeSeatMapper showtimeSeatMapper;
    private final SeatLockLogMapper seatLockLogMapper;
    private final OrderSnackService orderSnackService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public RefundPreparation prepare(Long userId, Long orderId) {
        TicketOrder order = orderMapper.selectForUpdate(orderId);
        if (order == null || !Objects.equals(order.getUserId(), userId)) {//查询订单
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }

        PaymentRefund latestRefund = findLatestRefund(orderId);//取最新一条退款记录的状态
        if ("REFUNDED".equals(order.getStatus())) {//已退票
            if (latestRefund != null) {
                return new RefundPreparation(latestRefund, false);
            }
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if ("REFUND_PENDING".equals(order.getStatus())) {//退款处理中
            if (latestRefund != null && PaymentRefund.PENDING.equals(latestRefund.getStatus())) {
                return new RefundPreparation(latestRefund, false);
            }
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (!"TICKETED".equals(order.getStatus())) {//没有付钱的状态,直接拒绝
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }

        Showtime showtime = showtimeMapper.selectById(order.getShowtimeId());
        if (showtime == null || showtime.getStartAt() == null || !showtime.getStartAt().isAfter(LocalDateTime.now())) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // 退票时间限制：距离开场不足30分钟不可退票
        long minutesUntilStart = java.time.Duration.between(LocalDateTime.now(), showtime.getStartAt()).toMinutes();
        if (minutesUntilStart < 30) {
            throw new OrderException(ErrorCode.ORDER_REFUND_TOO_LATE);
        }
        int feePercent = minutesUntilStart >= 24 * 60 ? 5 : 10;

        List<Ticket> tickets = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getOrderId, orderId));
        if (tickets.isEmpty() || tickets.stream().anyMatch(ticket -> !Integer.valueOf(TICKET_VALID).equals(ticket.getStatus()))) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }//电子票已使用状态不允许退款

        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .eq(Payment::getStatus, "SUCCESS")
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        if (payment == null || payment.getId() == null || payment.getAmount() == null
                || payment.getTradeNo() == null || payment.getTradeNo().isBlank()
                || payment.getOutTradeNo() == null || payment.getOutTradeNo().isBlank()
                || !Objects.equals(payment.getAmount(), order.getAmount())) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // 计算手续费和实退金额
        int originalAmountFen = payment.getAmount();
        BigDecimal amountYuan = new BigDecimal(originalAmountFen).movePointLeft(2);
        BigDecimal serviceFeeYuan = amountYuan.multiply(new BigDecimal(feePercent)).movePointLeft(2);
        BigDecimal refundYuan = amountYuan.subtract(serviceFeeYuan);
        int serviceFeeFen = serviceFeeYuan.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).intValue();
        int refundFen = refundYuan.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).intValue();

        PaymentRefund refund = new PaymentRefund();
        refund.setPaymentId(payment.getId());
        refund.setOrderId(orderId);
        refund.setOutRequestNo(buildOutRequestNo(order.getOrderNo()));
        refund.setOutTradeNo(payment.getOutTradeNo());
        refund.setTradeNo(payment.getTradeNo());
        refund.setRefundAmountFen(refundFen);
        refund.setServiceFeeFen(serviceFeeFen);
        refund.setStatus(PaymentRefund.PENDING);
        refund.setQueryCount(0);
        paymentRefundMapper.insert(refund);

        // 退款申请已受理后暂停电子票，防止处理中仍被使用。
        for (Ticket ticket : tickets) {
            ticket.setStatus(TICKET_REFUND_PENDING);
            ticketMapper.updateById(ticket);
        }
        order.setStatus("REFUND_PENDING");//更改为退票中的状态
        orderMapper.updateById(order);

        log.info("退款申请已冻结本地订单, orderId={}, refundId={}", orderId, refund.getId());
        return new RefundPreparation(refund, true);
    }

    @Override
    @Transactional
    public void settleSuccess(Long refundId, Integer actualAmountFen, String providerMessage) {
        PaymentRefund refund = paymentRefundMapper.selectForUpdate(refundId);
        if (refund == null || PaymentRefund.SUCCESS.equals(refund.getStatus())) {
            return;
        }
        if (!PaymentRefund.PENDING.equals(refund.getStatus())
                || actualAmountFen == null
                || !Objects.equals(refund.getRefundAmountFen(), actualAmountFen)) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }

        TicketOrder order = orderMapper.selectForUpdate(refund.getOrderId());
        if (order == null) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!"REFUND_PENDING".equals(order.getStatus()) && !"REFUNDED".equals(order.getStatus())) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if ("REFUNDED".equals(order.getStatus())) {
            markRefundSuccess(refund, actualAmountFen, providerMessage);
            return;
        }

        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        releaseRedisSeatLocks(order.getUserId(), order.getShowtimeId(), items);
        for (OrderItem item : items) {
            ShowtimeSeat seat = showtimeSeatMapper.selectById(item.getSeatId());
            if (seat != null && seat.getStatus() == 2) {
                seat.setStatus(0);
                seat.setLockOwner(null);
                seat.setLockExpiresAt(null);
                showtimeSeatMapper.updateById(seat);

                SeatLockLog seatLockLog = new SeatLockLog();
                seatLockLog.setOrderId(order.getId());
                seatLockLog.setShowtimeId(order.getShowtimeId());
                seatLockLog.setSeatId(item.getSeatId());
                seatLockLog.setAction("REFUND");
                seatLockLogMapper.insert(seatLockLog);
            }
        }

        // 仅处理 SOLD 明细，重复进入本事务不会重复回滚库存与销量。
        orderSnackService.refundSold(order.getId());

        List<Ticket> tickets = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getOrderId, order.getId()));
        for (Ticket ticket : tickets) {
            if (Integer.valueOf(TICKET_USED).equals(ticket.getStatus())) {
                throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
            }
            ticket.setStatus(TICKET_REFUNDED);
            ticketMapper.updateById(ticket);
        }

        Payment payment = paymentMapper.selectById(refund.getPaymentId());
        if (payment == null || !"SUCCESS".equals(payment.getStatus())) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }
        payment.setStatus("REFUNDED");
        paymentMapper.updateById(payment);

        order.setStatus("REFUNDED");
        orderMapper.updateById(order);

        Showtime showtime = showtimeMapper.selectById(order.getShowtimeId());
        if (showtime != null && showtime.getStatus() == ShowtimeStatus.SOLD_OUT_ALL) {
            showtime.setStatus(ShowtimeStatus.ON_SALE);
            showtimeMapper.updateById(showtime);
        }
        markRefundSuccess(refund, actualAmountFen, providerMessage);
        log.info("支付宝退款本地结算完成, orderId={}, refundId={}", order.getId(), refundId);
    }

    @Override
    @Transactional
    public void markFailure(Long refundId, String failureCode, String failureMessage) {
        PaymentRefund refund = paymentRefundMapper.selectForUpdate(refundId);
        if (refund == null || !PaymentRefund.PENDING.equals(refund.getStatus())) {
            return;
        }

        TicketOrder order = orderMapper.selectForUpdate(refund.getOrderId());
        if (order != null && "REFUND_PENDING".equals(order.getStatus())) {
            List<Ticket> tickets = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>()
                    .eq(Ticket::getOrderId, order.getId()));
            for (Ticket ticket : tickets) {
                if (Integer.valueOf(TICKET_REFUND_PENDING).equals(ticket.getStatus())) {
                    ticket.setStatus(TICKET_VALID);
                    ticketMapper.updateById(ticket);
                }
            }
            order.setStatus("TICKETED");
            orderMapper.updateById(order);
        }

        refund.setStatus(PaymentRefund.FAIL);
        refund.setFailureCode(trim(failureCode, 64));
        refund.setFailureMessage(trim(failureMessage, 255));
        refund.setProcessedAt(LocalDateTime.now());
        paymentRefundMapper.updateById(refund);
        log.warn("支付宝退款明确失败, refundId={}, code={}", refundId, failureCode);
    }

    @Override
    @Transactional
    public void recordPendingQuery(Long refundId, String failureCode, String message) {
        PaymentRefund refund = paymentRefundMapper.selectForUpdate(refundId);
        if (refund == null || !PaymentRefund.PENDING.equals(refund.getStatus())) {
            return;
        }
        refund.setQueryCount((refund.getQueryCount() == null ? 0 : refund.getQueryCount()) + 1);
        refund.setLastQueryAt(LocalDateTime.now());
        refund.setFailureCode(trim(failureCode, 64));
        refund.setFailureMessage(trim(message, 255));
        paymentRefundMapper.updateById(refund);
    }

    @Override
    public RefundResultVO getStatus(Long userId, Long orderId) {
        TicketOrder order = orderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        PaymentRefund refund = findLatestRefund(orderId);
        if (refund == null && "REFUNDED".equals(order.getStatus())) {
            RefundResultVO result = new RefundResultVO();
            result.setOrderId(orderId);
            result.setStatus(PaymentRefund.SUCCESS);
            result.setAmount(AmountUtil.yuan(order.getAmount()));
            result.setMessage("退款成功，电子票已失效");
            result.setUpdatedAt(order.getUpdateTime());
            return result;
        }
        if (refund == null) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }
        return toResult(refund);
    }

    @Override
    public List<PaymentRefund> listPending() {
        return paymentRefundMapper.selectList(new LambdaQueryWrapper<PaymentRefund>()
                .eq(PaymentRefund::getStatus, PaymentRefund.PENDING)
                .orderByAsc(PaymentRefund::getLastQueryAt)
                .orderByAsc(PaymentRefund::getId)
                .last("LIMIT 100"));
    }

    private PaymentRefund findLatestRefund(Long orderId) {
        return paymentRefundMapper.selectOne(new LambdaQueryWrapper<PaymentRefund>()
                .eq(PaymentRefund::getOrderId, orderId)
                .orderByDesc(PaymentRefund::getId)
                .last("LIMIT 1"));
    }

    private void markRefundSuccess(PaymentRefund refund, Integer actualAmountFen, String providerMessage) {
        refund.setStatus(PaymentRefund.SUCCESS);
        refund.setActualAmountFen(actualAmountFen);
        refund.setFailureCode(null);
        refund.setFailureMessage(null);
        refund.setProcessedAt(LocalDateTime.now());
        paymentRefundMapper.updateById(refund);
    }

    private void releaseRedisSeatLocks(Long userId, Long showtimeId, List<OrderItem> items) {
        if (userId == null || showtimeId == null || items == null || items.isEmpty()) {
            return;
        }
        items.stream()
                .map(OrderItem::getSeatId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(seatId -> stringRedisTemplate.execute(
                        RELEASE_LOCK_SCRIPT,
                        List.of(REDIS_LOCK_PREFIX + showtimeId + ":seat:" + seatId),
                        String.valueOf(userId)));
    }

    private static String buildOutRequestNo(String orderNo) {
        String prefix = orderNo == null || orderNo.isBlank() ? "ORDER" : orderNo;
        return prefix + "-R-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static RefundResultVO toResult(PaymentRefund refund) {
        RefundResultVO result = new RefundResultVO();
        result.setOrderId(refund.getOrderId());
        result.setStatus(refund.getStatus());
        result.setAmount(AmountUtil.yuan(refund.getRefundAmountFen()));
        result.setServiceFee(AmountUtil.yuan(refund.getServiceFeeFen()));
        result.setOutRequestNo(refund.getOutRequestNo());
        result.setUpdatedAt(refund.getUpdateTime() != null ? refund.getUpdateTime() : refund.getProcessedAt());
        if (PaymentRefund.SUCCESS.equals(refund.getStatus())) {
            result.setMessage("退款成功，电子票已失效");
        } else if (PaymentRefund.PENDING.equals(refund.getStatus())) {
            result.setMessage("退款处理中，请稍后刷新");
        } else {
            result.setMessage("退款失败，可重新申请退票");
        }
        return result;
    }
}
