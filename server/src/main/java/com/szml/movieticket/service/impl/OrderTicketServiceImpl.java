package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szml.movieticket.entity.*;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.OrderException;
import com.szml.movieticket.mapper.*;
import com.szml.movieticket.service.OrderTicketService;
import com.szml.movieticket.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    private final StringRedisTemplate stringRedisTemplate;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final com.szml.movieticket.mapper.TicketMapper ticketMapper;
    private final SeatLockLogMapper seatLockLogMapper;
    private final ShowtimeMapper showtimeMapper;
    private final ShowtimeSeatMapper showtimeSeatMapper;
    private final SeatMapper seatMapper;
    private final MovieMapper movieMapper;
    private final HallMapper hallMapper;
    private final CinemaMapper cinemaMapper;
    private final PurchaseDraftMapper draftMapper;

    @Override
    @Transactional
    public LockResultVO lockSeats(Long userId, Long showtimeId, List<Long> seatIds, Integer draftVersion) {
        // 校验场次存在且在售
        Showtime showtime = showtimeMapper.selectById(showtimeId);
        if (showtime == null) {
            throw new OrderException(ErrorCode.SHOWTIME_NOT_FOUND);
        }

        // 校验草稿版本
        PurchaseDraft draft = draftMapper.selectOne(new LambdaQueryWrapper<PurchaseDraft>()
                .eq(PurchaseDraft::getUserId, userId)
                .eq(PurchaseDraft::getStatus, "ACTIVE"));
        if (draft == null || !draft.getVersion().equals(draftVersion)) {
            throw new OrderException(ErrorCode.DRAFT_VERSION_CONFLICT);
        }

        // 校验座位状态（MySQL 查 seat 实际状态）
        List<ShowtimeSeat> seatInventories = showtimeSeatMapper.selectBatchIds(seatIds);
        if (seatInventories.size() != seatIds.size()) {
            throw new OrderException(ErrorCode.SHOWTIME_SEAT_NOT_FOUND);
        }
        // 确保座位属于该场次
        for (ShowtimeSeat inventory : seatInventories) {
            if (!inventory.getShowtimeId().equals(showtimeId)) {
                throw new OrderException(ErrorCode.SHOWTIME_SEAT_NOT_FOUND);
            }
        }

        // TODO: 后续接入 Redis Lua 原子占座，当前使用 MySQL SELECT ... FOR UPDATE
        // 校验全部 AVAILABLE 或 COUPLE
        List<ShowtimeSeat> locked = showtimeSeatMapper.selectList(
                new LambdaQueryWrapper<ShowtimeSeat>()
                        .in(ShowtimeSeat::getId, seatIds)
                        .last("FOR UPDATE"));
        for (ShowtimeSeat seat : locked) {
            int status = seat.getStatus() != null ? seat.getStatus() : 0;
            if (status == 1 || status == 2) {
                throw new OrderException(ErrorCode.SEAT_LOCK_CONFLICT);
            }
        }

        // 锁定座位
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(LOCK_SECONDS);
        for (ShowtimeSeat seat : locked) {
            seat.setStatus(1);
            seat.setLockOwner(userId);
            seat.setLockExpiresAt(expiresAt);
            seat.setVersion(seat.getVersion() + 1);
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
        order.setStatus("PAYMENT_PENDING");
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
        draft.setStatus("FROZEN");
        draft.setOrderId(order.getId());
        draft.setVersion(draft.getVersion() + 1);
        draftMapper.updateById(draft);

        log.info("锁座成功, userId: {}, orderId: {}, orderNo: {}, seatIds: {}", userId, order.getId(), orderNo, seatIds);

        // 构建返回 VO
        LockResultVO result = new LockResultVO();
        result.setOrderId(order.getId());
        result.setOrderNo(orderNo);
        result.setAmount(yuan(amount));
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
            si.setPrice(yuan(s.getPrice() != null ? s.getPrice() : showtime.getBasePrice()));
            seatInfos.add(si);
        }
        result.setSeats(seatInfos);

        return result;
    }

    @Override
    @Transactional
    public PayResultVO pay(Long userId, Long orderId, String idempotencyKey) {
        TicketOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }
        if ("PAID".equals(order.getStatus()) || "TICKETED".equals(order.getStatus())) {
            Payment existing = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                    .eq(Payment::getOrderId, orderId)
                    .eq(Payment::getIdempotencyKey, idempotencyKey));
            if (existing != null) {
                throw new OrderException(ErrorCode.PAYMENT_IDEMPOTENT_REPLAY);
            }
        }
        if (!"PAYMENT_PENDING".equals(order.getStatus())) {
            throw new OrderException(ErrorCode.ORDER_STATUS_INVALID);
        }
        if (order.getExpiresAt() != null && order.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OrderException(ErrorCode.ORDER_EXPIRED);
        }

        // 写支付记录
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setStatus("SUCCESS");
        payment.setAmount(order.getAmount());
        payment.setProcessedAt(LocalDateTime.now());
        paymentMapper.insert(payment);

        // 订单 → PAID
        order.setStatus("PAID");
        orderMapper.updateById(order);

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
        List<PayResultVO.TicketItem> ticketItems = new ArrayList<>();
        for (OrderItem item : items) {
            ShowtimeSeat sts = showtimeSeatMapper.selectById(item.getSeatId());
            Seat physicalSeat = sts != null ? seatMapper.selectById(sts.getSeatId()) : null;
            String ticketCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

            Ticket ticket = new Ticket();
            ticket.setOrderId(orderId);
            ticket.setOrderItemId(item.getId());
            ticket.setTicketCode(ticketCode);

            Showtime showtime = showtimeMapper.selectById(order.getShowtimeId());
            Movie movie = showtime != null ? movieMapper.selectById(showtime.getMovieId()) : null;
            Hall hall = showtime != null ? hallMapper.selectById(showtime.getHallId()) : null;
            Cinema cinema = hall != null ? cinemaMapper.selectById(hall.getCinemaId()) : null;

            // 组装 qr_content
            Map<String, String> qrMap = new LinkedHashMap<>();
            qrMap.put("movie", movie != null ? movie.getName() : "");
            qrMap.put("cinema", cinema != null ? cinema.getName() : "");
            qrMap.put("hall", hall != null ? hall.getName() : "");
            qrMap.put("row", physicalSeat != null ? String.valueOf(physicalSeat.getRowNo()) : "");
            qrMap.put("seat", physicalSeat != null ? String.valueOf(physicalSeat.getSeatNo()) : "");
            qrMap.put("startAt", showtime != null ? showtime.getStartAt().toString() : "");
            qrMap.put("ticketCode", ticketCode);
            ticket.setQrContent(new cn.hutool.json.JSONObject(qrMap).toString());
            ticket.setStatus(0);
            ticketMapper.insert(ticket);

            PayResultVO.TicketItem ticketItem = new PayResultVO.TicketItem();
            ticketItem.setTicketCode(ticketCode);
            ticketItem.setSeat((physicalSeat != null ? physicalSeat.getRowNo() : "") + "排"
                    + (physicalSeat != null ? physicalSeat.getSeatNo() : "") + "座");
            ticketItem.setQrContent(ticket.getQrContent());
            ticketItems.add(ticketItem);
        }

        // 订单 → TICKETED
        order.setStatus("TICKETED");
        orderMapper.updateById(order);

        log.info("支付成功, userId: {}, orderId: {}, idempotencyKey: {}", userId, orderId, idempotencyKey);

        PayResultVO result = new PayResultVO();
        result.setOrderId(orderId);
        result.setStatus("TICKETED");
        result.setPaidAmount(yuan(order.getAmount()));
        result.setTickets(ticketItems);
        return result;
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        TicketOrder order = orderMapper.selectById(orderId);
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
            vo.setAmount(yuan(order.getAmount()));
            vo.setStatus(order.getStatus());
            vo.setStatusDesc(statusDesc(order.getStatus()));
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
        vo.setAmount(yuan(order.getAmount()));
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(statusDesc(order.getStatus()));
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
            itemVO.setUnitPrice(yuan(item.getUnitPrice()));
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

        // 支付
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, orderId));
        if (payment != null) {
            UserOrderDetailVO.PaymentInfo pi = new UserOrderDetailVO.PaymentInfo();
            pi.setStatus(payment.getStatus());
            pi.setAmount(yuan(payment.getAmount()));
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

    private static double yuan(int cents) {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String statusDesc(String status) {
        return switch (status) {
            case "PAYMENT_PENDING" -> "待支付";
            case "PAID" -> "已支付";
            case "TICKETED" -> "已出票";
            case "CANCELLED" -> "已取消";
            case "EXPIRED" -> "已过期";
            default -> status;
        };
    }
}
