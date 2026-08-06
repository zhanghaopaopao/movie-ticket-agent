package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szml.movieticket.entity.*;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.enums.ShowtimeStatus;
import com.szml.movieticket.exception.OrderException;
import com.szml.movieticket.mapper.*;
import com.szml.movieticket.service.OrderTicketService;
import com.szml.movieticket.service.AlipayPaymentService;
import com.szml.movieticket.service.OrderSnackService;
import com.szml.movieticket.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.szml.movieticket.util.AmountUtil;
import com.szml.movieticket.util.OrderStatusUtil;

/**
 * C 端订单与支付服务实现类。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTicketServiceImpl implements OrderTicketService {

    private static final int LOCK_SECONDS = 900;
    private static final String REDIS_LOCK_PREFIX = "lock:showtime:";

    /**
     * Redis Lua 脚本：批量原子占座。
     * KEYS[] = 要锁定的 Redis key 列表
     * ARGV[1] = userId
     * ARGV[2] = TTL 秒数
     * 返回：{1} 表示全部占座成功；{0, 冲突key} 表示存在冲突
     */
    private static final String LOCK_LUA_SCRIPT = """
            local userId = ARGV[1]
            local ttl = tonumber(ARGV[2])
            for i = 1, #KEYS do
                local ok = redis.call('SET', KEYS[i], userId, 'NX', 'EX', ttl)
                if not ok then
                    for j = 1, i - 1 do
                        redis.call('DEL', KEYS[j])
                    end
                    return {0, KEYS[i]}
                end
            end
            return {1}
            """;

    private final StringRedisTemplate stringRedisTemplate;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final TicketMapper ticketMapper;
    private final SeatLockLogMapper seatLockLogMapper;
    private final ShowtimeMapper showtimeMapper;
    private final ShowtimeSeatMapper showtimeSeatMapper;
    private final SeatMapper seatMapper;
    private final MovieMapper movieMapper;
    private final HallMapper hallMapper;
    private final CinemaMapper cinemaMapper;
    private final PurchaseDraftMapper draftMapper;
    private final AlipayPaymentService alipayPaymentService;
    private final OrderSnackService orderSnackService;

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public LockResultVO lockSeats(Long userId, Long showtimeId, List<Long> seatIds, Integer draftVersion) {
        // 校验场次存在且在售
        Showtime showtime = showtimeMapper.selectById(showtimeId);
        if (showtime == null) {
            throw new OrderException(ErrorCode.SHOWTIME_NOT_FOUND);
        }
        if (showtime.getStatus() != ShowtimeStatus.ON_SALE) {
            log.warn("场次不在售, showtimeId: {}, status: {}", showtimeId, showtime.getStatus());
            throw new OrderException(ErrorCode.SHOWTIME_NOT_FOUND);
        }
        if (showtime.getStartAt() == null || !showtime.getStartAt().isAfter(LocalDateTime.now())) {
            log.warn("场次已开始，拒绝锁座, showtimeId: {}, startAt: {}", showtimeId, showtime.getStartAt());
            throw new OrderException(ErrorCode.SHOWTIME_ALREADY_STARTED);
        }

        // 校验草稿版本
        PurchaseDraft draft = draftMapper.selectOne(new LambdaQueryWrapper<PurchaseDraft>()
                .eq(PurchaseDraft::getUserId, userId)
                .eq(PurchaseDraft::getStatus, "ACTIVE"));
        if (draft == null || !draft.getVersion().equals(draftVersion)) {
            throw new OrderException(ErrorCode.DRAFT_VERSION_CONFLICT);
        }

        // 第1层：Redis Lua 原子占座（批量 SETNX，任一冲突则全部回退）
        List<String> redisKeys = seatIds.stream()
                .map(sid -> REDIS_LOCK_PREFIX + showtimeId + ":seat:" + sid)
                .toList();
        DefaultRedisScript<List> script = new DefaultRedisScript<>(LOCK_LUA_SCRIPT, List.class);
        List<Object> redisResult = stringRedisTemplate.execute(
                script, redisKeys, String.valueOf(userId), String.valueOf(LOCK_SECONDS));
        if (redisResult == null || redisResult.isEmpty() || !redisResult.get(0).equals(1L)) {
            log.warn("Redis占座冲突, 用户ID: {}, 场次ID: {}, 冲突Key: {}", userId, showtimeId,
                    redisResult != null && redisResult.size() > 1 ? redisResult.get(1) : "unknown");
            throw new OrderException(ErrorCode.SEAT_LOCK_CONFLICT);
        }
        log.debug("Redis原子占座成功, 用户ID: {}, 场次ID: {}, 座位数: {}", userId, showtimeId, seatIds.size());

        // 第2层：MySQL SELECT ... FOR UPDATE（按 seatId ASC 排序，防死锁 + 持久化兜底）
        List<ShowtimeSeat> locked = showtimeSeatMapper.selectForUpdate(showtimeId, seatIds);
        if (locked.size() != seatIds.size()) {
            // Redis 占座成功但 MySQL 记录不存在 → 释放 Redis 并抛异常
            redisKeys.forEach(k -> stringRedisTemplate.delete(k));
            throw new OrderException(ErrorCode.SHOWTIME_SEAT_NOT_FOUND);
        }
        for (ShowtimeSeat seat : locked) {
            int status = seat.getStatus() != null ? seat.getStatus() : 0;
            if (status == 1 || status == 2) {
                // 极端情况：Redis TTL 刚好过期 + 被另一个事务抢先写入
                redisKeys.forEach(k -> stringRedisTemplate.delete(k));
                throw new OrderException(ErrorCode.SEAT_LOCK_CONFLICT);
            }
            seat.setVersion(seat.getVersion() + 1);
        }

        // 锁定座位
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(LOCK_SECONDS);
        for (ShowtimeSeat seat : locked) {
            seat.setStatus(1);
            seat.setLockOwner(userId);
            seat.setLockExpiresAt(expiresAt);
            showtimeSeatMapper.updateById(seat);
        }

        // 生成订单号
        String orderNo = generateOrderNo();

        // 计算金额
        int amount = locked.stream().mapToInt(s -> s.getPrice() != null ? s.getPrice() : showtime.getBasePrice()).sum();

        // 创建订单
        TicketOrder order = new TicketOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setShowtimeId(showtimeId);
        order.setAmount(amount);
        order.setStatus("PAYMENT_PENDING");//设置状态为待支付
        order.setExpiresAt(expiresAt);
        order.setRetryCount(0);
        orderMapper.insert(order);

        // 创建订单明细
        for (ShowtimeSeat seat : locked) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setSeatId(seat.getId());
            item.setUnitPrice(seat.getPrice() != null ? seat.getPrice() : showtime.getBasePrice());
            orderItemMapper.insert(item);

            // 写锁座审计日志
            SeatLockLog log1 = new SeatLockLog();
            log1.setOrderId(order.getId());
            log1.setShowtimeId(showtimeId);
            log1.setSeatId(seat.getId());
            log1.setAction("LOCK");
            seatLockLogMapper.insert(log1);
        }

        // 冻结草稿
        draft.setStatus("FROZEN");//草稿冻结状态
        draft.setOrderId(order.getId());
        draft.setVersion(draft.getVersion() + 1);
        draftMapper.updateById(draft);

        log.info("锁座成功, userId: {}, orderId: {}, orderNo: {}, seatIds: {}", userId, order.getId(), orderNo, seatIds);

        // 构建返回 VO
        LockResultVO result = new LockResultVO();
        result.setOrderId(order.getId());
        result.setOrderNo(orderNo);
        result.setAmount(AmountUtil.yuan(amount));
        result.setExpiresAt(expiresAt);
        result.setRemainingSeconds((long) LOCK_SECONDS);

        Movie movie = movieMapper.selectById(showtime.getMovieId());
        Hall hall = hallMapper.selectById(showtime.getHallId());
        Cinema cinema = hall != null ? cinemaMapper.selectById(hall.getCinemaId()) : null;

        if (movie != null) {
            LockResultVO.MovieBriefVO mb = new LockResultVO.MovieBriefVO();
            mb.setId(movie.getId()); mb.setName(movie.getName());
            result.setMovie(mb);
        }
        if (cinema != null) {
            LockResultVO.CinemaBriefVO cb = new LockResultVO.CinemaBriefVO();
            cb.setId(cinema.getId()); cb.setName(cinema.getName());
            result.setCinema(cb);
        }
        result.setHallName(hall != null ? hall.getName() : null);
        result.setStartAt(showtime.getStartAt());

        List<LockResultVO.SeatInfo> seatInfos = new ArrayList<>();
        for (ShowtimeSeat s : locked) {
            Seat physicalSeat = seatMapper.selectById(s.getSeatId());
            LockResultVO.SeatInfo si = new LockResultVO.SeatInfo();
            si.setRowNo(physicalSeat != null ? physicalSeat.getRowNo() : null);
            si.setSeatNo(physicalSeat != null ? physicalSeat.getSeatNo() : null);
            si.setPrice(AmountUtil.yuan(s.getPrice() != null ? s.getPrice() : showtime.getBasePrice()));
            seatInfos.add(si);
        }
        result.setSeats(seatInfos);

        return result;
    }

    @Override
    @Transactional
    public PaymentInitVO createPayment(Long userId, Long orderId, String idempotencyKey) {
//        return createPaymentInternal(userId, orderId, idempotencyKey, false);
//    }
//
//    @Override
//    @Transactional
//    public PaymentInitVO createQrPayment(Long userId, Long orderId, String idempotencyKey) {
//        return createPaymentInternal(userId, orderId, idempotencyKey, true);
//    }
//
//    private PaymentInitVO createPaymentInternal(Long userId, Long orderId, String idempotencyKey,
//                                                boolean qrCodePayment) {
        TicketOrder order = orderMapper.selectForUpdate(orderId);
        if (order == null) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }

        PaymentInitVO result = new PaymentInitVO();
        result.setOrderId(orderId);
        result.setOutTradeNo(order.getOrderNo());

        // 已完成订单不再创建新的支付交易。
        if ("PAID".equals(order.getStatus()) || "TICKETED".equals(order.getStatus())) {
            result.setPaymentStatus("SUCCESS");
            result.setPayForm("");
            result.setQrCode("");
            return result;
        }

        if (!"PAYMENT_PENDING".equals(order.getStatus())) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (order.getExpiresAt() != null && order.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OrderException(ErrorCode.ORDER_EXPIRED);
        }

        Showtime showtime = showtimeMapper.selectById(order.getShowtimeId());
        Movie movie = showtime != null ? movieMapper.selectById(showtime.getMovieId()) : null;
        Hall hall = showtime != null ? hallMapper.selectById(showtime.getHallId()) : null;
        Cinema cinema = hall != null ? cinemaMapper.selectById(hall.getCinemaId()) : null;
        String subject = (movie != null ? movie.getName() : "电影票")
                + " - " + (cinema != null ? cinema.getName() : "影院");

        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(orderId);
        }
        String requestIdempotencyKey = idempotencyKey == null ? "" : idempotencyKey.trim();
        boolean sameAttempt = "PENDING".equals(payment.getStatus())
                && !requestIdempotencyKey.isBlank()
                && requestIdempotencyKey.equals(payment.getIdempotencyKey())
                && payment.getOutTradeNo() != null
                && !payment.getOutTradeNo().isBlank();
        String outTradeNo = sameAttempt
                ? payment.getOutTradeNo()
                : buildPaymentTradeNo(order.getOrderNo());
        if (!sameAttempt) {
            // 支付宝沙箱可能保留上一次异常跳转的待支付交易，新尝试必须使用新的商户订单号。
            payment.setTradeNo(null);
        }
        payment.setProvider("ALIPAY_SANDBOX");
        payment.setOutTradeNo(outTradeNo);
        payment.setSubject(subject);
        payment.setIdempotencyKey(requestIdempotencyKey);
        payment.setStatus("PENDING");
        payment.setAmount(order.getAmount());
        payment.setProcessedAt(null);
        payment.setNotifyTime(null);
        if (payment.getId() == null) {
            paymentMapper.insert(payment);
        } else {
            paymentMapper.updateById(payment);
        }

        result.setOutTradeNo(outTradeNo);
        result.setPaymentStatus("PENDING");
//        if (qrCodePayment) {
//            result.setQrCode(alipayPaymentService.createPrecreateQrCode(outTradeNo, subject, order.getAmount()));
//        } else {
            result.setPayForm(alipayPaymentService.createWapPayForm(outTradeNo, subject, order.getAmount()));
//        }
        return result;
    }

    @Override
    @Transactional
    public void handleAlipaySuccess(String outTradeNo, String tradeNo, BigDecimal totalAmount,
                                    String notifyTime) {
        if (outTradeNo == null || outTradeNo.isBlank() || tradeNo == null || tradeNo.isBlank()) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOutTradeNo, outTradeNo)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        if (payment == null) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (payment.getAmount() == null) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }
        BigDecimal expected = BigDecimal.valueOf(payment.getAmount(), 2);
        if (totalAmount == null || expected.compareTo(totalAmount.setScale(2, RoundingMode.HALF_UP)) != 0) {
            log.warn("支付宝通知金额不匹配, outTradeNo={}, expected={}, actual={}", outTradeNo, expected, totalAmount);
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }
        TicketOrder order = orderMapper.selectForUpdate(payment.getOrderId());
        if (order == null) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        if ("TICKETED".equals(order.getStatus()) || "PAID".equals(order.getStatus())) {
            payment.setTradeNo(tradeNo);
            payment.setStatus("SUCCESS");
            payment.setProcessedAt(payment.getProcessedAt() != null ? payment.getProcessedAt() : LocalDateTime.now());
            payment.setNotifyTime(parseNotifyTime(notifyTime));
            paymentMapper.updateById(payment);
            return;
        }
        if (!"PAYMENT_PENDING".equals(order.getStatus())) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // 当前事务已持有订单行锁，重复通知不会重复出票。
        completePaidOrder(order.getUserId(), order.getId(), "alipay-" + tradeNo);
        Payment processed = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, order.getId())
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        if (processed != null) {
            processed.setProvider("ALIPAY_SANDBOX");
            processed.setOutTradeNo(outTradeNo);
            processed.setTradeNo(tradeNo);
            processed.setStatus("SUCCESS");
            processed.setNotifyTime(parseNotifyTime(notifyTime));
            paymentMapper.updateById(processed);
        }
    }

    @Override
    @Transactional
    public void handleAlipayClosed(String outTradeNo, String notifyTime) {
        if (outTradeNo == null || outTradeNo.isBlank()) {
            return;
        }
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOutTradeNo, outTradeNo)
                .last("LIMIT 1"));
        if (payment != null && "PENDING".equals(payment.getStatus())) {
            payment.setStatus("CLOSED");
            payment.setNotifyTime(parseNotifyTime(notifyTime));
            paymentMapper.updateById(payment);
        }
    }

    /**
     * 仅由支付宝验签后的成功通知调用，完成订单、座位和出票状态变更。
     */
    private PayResultVO completePaidOrder(Long userId, Long orderId, String idempotencyKey) {
        TicketOrder order = orderMapper.selectForUpdate(orderId);
        if (order == null) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }

        // 幂等：已支付/已出票的订单直接返回已有结果
        if ("PAID".equals(order.getStatus()) || "TICKETED".equals(order.getStatus())) {
            log.info("订单已支付，返回已有结果, orderId: {}", orderId);
            return buildPaidResult(order);
        }

        if (!"PAYMENT_PENDING".equals(order.getStatus())) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (order.getExpiresAt() != null && order.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OrderException(ErrorCode.ORDER_EXPIRED);
        }

        // 写入或更新支付记录。真正的成功状态由支付宝通知负责确认。
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        if (payment == null) {
            payment = new Payment();
            payment.setOrderId(orderId);
        }
        payment.setIdempotencyKey(idempotencyKey);
        payment.setStatus("SUCCESS");
        payment.setAmount(order.getAmount());
        payment.setProcessedAt(LocalDateTime.now());
        if (payment.getId() == null) {
            paymentMapper.insert(payment);
        } else {
            paymentMapper.updateById(payment);
        }

        // 订单 → PAID
        order.setStatus("PAID");
        orderMapper.updateById(order);

        // 支付成功后把预占库存转为售出，并累计商品销量。
        orderSnackService.markSold(orderId);

        // 座位 → SOLD
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            ShowtimeSeat seat = showtimeSeatMapper.selectById(item.getSeatId());
            if (seat != null) {
                seat.setStatus(2);
                showtimeSeatMapper.updateById(seat);
            }
        }

        // 出票
        Showtime payShowtime = showtimeMapper.selectById(order.getShowtimeId());
        Movie payMovie = payShowtime != null ? movieMapper.selectById(payShowtime.getMovieId()) : null;
        Hall payHall = payShowtime != null ? hallMapper.selectById(payShowtime.getHallId()) : null;
        Cinema payCinema = payHall != null ? cinemaMapper.selectById(payHall.getCinemaId()) : null;

        for (OrderItem item : items) {
            ShowtimeSeat sts = showtimeSeatMapper.selectById(item.getSeatId());
            Seat physicalSeat = sts != null ? seatMapper.selectById(sts.getSeatId()) : null;
            String ticketCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

            Ticket ticket = new Ticket();
            ticket.setOrderId(orderId);
            ticket.setOrderItemId(item.getId());
            ticket.setTicketCode(ticketCode);

            Map<String, String> qrMap = new LinkedHashMap<>();
            qrMap.put("movie", payMovie != null ? payMovie.getName() : "");
            qrMap.put("cinema", payCinema != null ? payCinema.getName() : "");
            qrMap.put("hall", payHall != null ? payHall.getName() : "");
            qrMap.put("row", physicalSeat != null ? String.valueOf(physicalSeat.getRowNo()) : "");
            qrMap.put("seat", physicalSeat != null ? String.valueOf(physicalSeat.getSeatNo()) : "");
            qrMap.put("startAt", payShowtime != null ? payShowtime.getStartAt().toString() : "");
            qrMap.put("ticketCode", ticketCode);
            ticket.setQrContent(new cn.hutool.json.JSONObject(qrMap).toString());
            ticket.setStatus(0);
            ticketMapper.insert(ticket);
        }

        // 订单 → TICKETED
        order.setStatus("TICKETED");
        orderMapper.updateById(order);

        log.info("支付成功, userId: {}, orderId: {}, idempotencyKey: {}", userId, orderId, idempotencyKey);
        return buildPaidResult(order);
    }

    /**
     * 构建已支付/已出票订单的结果 VO（幂等返回用）。
     */
    private PayResultVO buildPaidResult(TicketOrder order) {
        List<PayResultVO.TicketItem> ticketItems = ticketMapper.selectList(
                        new LambdaQueryWrapper<Ticket>().eq(Ticket::getOrderId, order.getId()))
                .stream().map(ticket -> {
                    PayResultVO.TicketItem item = new PayResultVO.TicketItem();
                    item.setTicketCode(ticket.getTicketCode());
                    item.setQrContent(ticket.getQrContent());
                    OrderItem oi = orderItemMapper.selectById(ticket.getOrderItemId());
                    if (oi != null) {
                        ShowtimeSeat sts = showtimeSeatMapper.selectById(oi.getSeatId());
                        if (sts != null) {
                            Seat physicalSeat = seatMapper.selectById(sts.getSeatId());
                            item.setSeat((physicalSeat != null ? physicalSeat.getRowNo() : "") + "排"
                                    + (physicalSeat != null ? physicalSeat.getSeatNo() : "") + "座");
                        }
                    }
                    return item;
                }).toList();

        PayResultVO result = new PayResultVO();
        result.setOrderId(order.getId());
        result.setStatus(order.getStatus());
        result.setPaidAmount(AmountUtil.yuan(order.getAmount()));
        result.setTickets(ticketItems);
        return result;
    }

    private static LocalDateTime parseNotifyTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (RuntimeException ignored) {
            return LocalDateTime.now();
        }
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        TicketOrder order = orderMapper.selectForUpdate(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!"PAYMENT_PENDING".equals(order.getStatus())) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // 释放座位
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            ShowtimeSeat seat = showtimeSeatMapper.selectById(item.getSeatId());
            if (seat != null && seat.getStatus() == 1) {
                seat.setStatus(0);
                seat.setLockOwner(null);
                seat.setLockExpiresAt(null);
                showtimeSeatMapper.updateById(seat);
            }
            SeatLockLog log1 = new SeatLockLog();
            log1.setOrderId(orderId);
            log1.setShowtimeId(order.getShowtimeId());
            log1.setSeatId(item.getSeatId());
            log1.setAction("RELEASE");
            seatLockLogMapper.insert(log1);
        }

        order.setStatus("CANCELLED");
        orderMapper.updateById(order);

        // 取消订单时释放预占的零食库存，保留明细快照。
        orderSnackService.releaseReserved(orderId);

        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .orderByDesc(Payment::getId)
                .last("LIMIT 1"));
        if (payment != null && "PENDING".equals(payment.getStatus())) {
            payment.setStatus("CLOSED");
            paymentMapper.updateById(payment);
        }

        // 解冻草稿
        PurchaseDraft draft = draftMapper.selectOne(new LambdaQueryWrapper<PurchaseDraft>()
                .eq(PurchaseDraft::getUserId, userId)
                .eq(PurchaseDraft::getOrderId, orderId));
        if (draft != null) {
            draft.setStatus("ACTIVE");
            draft.setOrderId(null);
            draft.setVersion(draft.getVersion() + 1);
            draftMapper.updateById(draft);
        }

        log.info("取消订单成功, userId: {}, orderId: {}", userId, orderId);
    }

    @Override
    public UserOrderPageVO listOrders(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<TicketOrder> wrapper = new LambdaQueryWrapper<TicketOrder>()
                .eq(TicketOrder::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(TicketOrder::getStatus, status);
        }
        wrapper.orderByDesc(TicketOrder::getCreateTime);

        Page<TicketOrder> pageResult = new Page<>(page, size);
        orderMapper.selectPage(pageResult, wrapper);

        List<UserOrderVO> records = pageResult.getRecords().stream().map(order -> {
            UserOrderVO vo = new UserOrderVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setAmount(AmountUtil.yuan(order.getAmount()));
            vo.setStatus(order.getStatus());
            vo.setStatusDesc(OrderStatusUtil.statusDesc(order.getStatus()));
            vo.setExpiresAt(order.getExpiresAt());
            vo.setCreateTime(order.getCreateTime());

            Showtime showtime = showtimeMapper.selectById(order.getShowtimeId());
            if (showtime != null) {
                vo.setStartAt(showtime.getStartAt());
                Movie movie = movieMapper.selectById(showtime.getMovieId());
                if (movie != null) vo.setMovieName(movie.getName());
                Hall hall = hallMapper.selectById(showtime.getHallId());
                if (hall != null) {
                    vo.setHallName(hall.getName());
                    Cinema cinema = cinemaMapper.selectById(hall.getCinemaId());
                    if (cinema != null) vo.setCinemaName(cinema.getName());
                }
            }

            // 座位摘要
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            List<String> seatLabels = new ArrayList<>();
            for (OrderItem item : items) {
                ShowtimeSeat sts = showtimeSeatMapper.selectById(item.getSeatId());
                if (sts != null) {
                    Seat seat = seatMapper.selectById(sts.getSeatId());
                    if (seat != null) {
                        seatLabels.add(seat.getRowNo() + "排" + seat.getSeatNo() + "座");
                    }
                }
            }
            vo.setSeatSummary(String.join(", ", seatLabels));
            return vo;
        }).collect(Collectors.toList());

        UserOrderPageVO pageVO = new UserOrderPageVO();
        pageVO.setTotal(pageResult.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setRecords(records);
        return pageVO;
    }

    @Override
    public UserOrderDetailVO getOrderDetail(Long userId, Long orderId) {
        TicketOrder order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }

        UserOrderDetailVO vo = new UserOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setAmount(AmountUtil.yuan(order.getAmount()));
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatusUtil.statusDesc(order.getStatus()));
        vo.setExpiresAt(order.getExpiresAt());
        vo.setCreateTime(order.getCreateTime());

        Showtime showtime = showtimeMapper.selectById(order.getShowtimeId());
        if (showtime != null) {
            vo.setStartAt(showtime.getStartAt());
            vo.setEndAt(showtime.getEndAt());
            vo.setLanguage(showtime.getLanguage());

            Movie movie = movieMapper.selectById(showtime.getMovieId());
            if (movie != null) {
                UserOrderDetailVO.MovieBrief mb = new UserOrderDetailVO.MovieBrief();
                mb.setId(movie.getId()); mb.setName(movie.getName()); mb.setPoster(movie.getPoster());
                vo.setMovie(mb);
            }
            Hall hall = hallMapper.selectById(showtime.getHallId());
            if (hall != null) {
                vo.setHallName(hall.getName());
                vo.setHallType(hall.getHallType() != null ? hall.getHallType().getDesc() : null);
                Cinema cinema = cinemaMapper.selectById(hall.getCinemaId());
                if (cinema != null) {
                    UserOrderDetailVO.CinemaBrief cb = new UserOrderDetailVO.CinemaBrief();
                    cb.setId(cinema.getId()); cb.setName(cinema.getName()); cb.setAddress(cinema.getAddress());
                    vo.setCinema(cb);
                }
            }
        }

        // 订单明细
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        List<UserOrderDetailVO.OrderItemInfo> itemVOs = new ArrayList<>();
        for (OrderItem item : items) {
            UserOrderDetailVO.OrderItemInfo itemVO = new UserOrderDetailVO.OrderItemInfo();
            itemVO.setUnitPrice(AmountUtil.yuan(item.getUnitPrice()));
            ShowtimeSeat sts = showtimeSeatMapper.selectById(item.getSeatId());
            if (sts != null) {
                Seat seat = seatMapper.selectById(sts.getSeatId());
                if (seat != null) {
                    itemVO.setRowNo(seat.getRowNo());
                    itemVO.setSeatNo(seat.getSeatNo());
                    itemVO.setZone(seat.getZone());
                }
            }
            List<Ticket> tickets = ticketMapper.selectList(
                    new LambdaQueryWrapper<Ticket>().eq(Ticket::getOrderItemId, item.getId()));
            if (!tickets.isEmpty()) {
                itemVO.setTicketCode(tickets.get(0).getTicketCode());
            }
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);
        vo.setSnacks(orderSnackService.listOrderItems(orderId));
        vo.setSnackAmount(AmountUtil.yuan(orderSnackService.getSnackAmountFen(orderId)));

        // 支付
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, orderId));
        if (payment != null) {
            UserOrderDetailVO.PaymentInfo pi = new UserOrderDetailVO.PaymentInfo();
            pi.setStatus(payment.getStatus());
            pi.setAmount(AmountUtil.yuan(payment.getAmount()));
            pi.setProcessedAt(payment.getProcessedAt());
            vo.setPayment(pi);
        }

        // 电子票
        List<Ticket> tickets = ticketMapper.selectList(
                new LambdaQueryWrapper<Ticket>().eq(Ticket::getOrderId, orderId));
        List<UserOrderDetailVO.TicketInfo> ticketVOs = new ArrayList<>();
        for (Ticket ticket : tickets) {
            UserOrderDetailVO.TicketInfo ti = new UserOrderDetailVO.TicketInfo();
            ti.setTicketCode(ticket.getTicketCode());
            ti.setQrContent(ticket.getQrContent());
            OrderItem oi = orderItemMapper.selectById(ticket.getOrderItemId());
            if (oi != null) {
                ShowtimeSeat sts = showtimeSeatMapper.selectById(oi.getSeatId());
                if (sts != null) {
                    Seat seat = seatMapper.selectById(sts.getSeatId());
                    if (seat != null) {
                        ti.setRowNo(seat.getRowNo());
                        ti.setSeatNo(seat.getSeatNo());
                    }
                }
            }
            ticketVOs.add(ti);
        }
        vo.setTickets(ticketVOs);

        return vo;
    }

    private static String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        StringBuilder code = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 6; i++) code.append(random.nextInt(10));
        return timestamp + code;
    }

    /** 生成每次支付宝支付尝试使用的唯一商户订单号。 */
    private static String buildPaymentTradeNo(String orderNo) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return orderNo + "-" + suffix;
    }

}
