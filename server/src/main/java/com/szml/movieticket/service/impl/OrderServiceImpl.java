package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.entity.*;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.OrderException;
import com.szml.movieticket.mapper.*;
import com.szml.movieticket.service.OrderService;
import com.szml.movieticket.vo.OrderDetailVO;
import com.szml.movieticket.vo.OrderPageVO;
import com.szml.movieticket.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.szml.movieticket.util.AmountUtil;import com.szml.movieticket.util.OrderStatusUtil;

/**
 * 订单服务实现类（B 端只读）。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, TicketOrder> implements OrderService {

    private final UserMapper userMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final TicketMapper ticketMapper;
    private final SeatLockLogMapper seatLockLogMapper;
    private final ShowtimeMapper showtimeMapper;
    private final MovieMapper movieMapper;
    private final HallMapper hallMapper;
    private final CinemaMapper cinemaMapper;
    private final ShowtimeSeatMapper showtimeSeatMapper;
    private final SeatMapper seatMapper;


    @Override
    public OrderPageVO pageOrders(int pageNum, int size, String orderNo, String email,
                                   Long movieId, Long cinemaId, String status,
                                   String startDate, String endDate) {
        // email → userId
        if (StringUtils.hasText(email)) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (user == null) {
                OrderPageVO empty = new OrderPageVO();
                empty.setTotal(0); empty.setPage(pageNum); empty.setSize(size);
                empty.setRecords(new ArrayList<>());
                return empty;
            }
            LambdaQueryWrapper<TicketOrder> wrapper = new LambdaQueryWrapper<TicketOrder>().eq(TicketOrder::getUserId, user.getId());
            return buildPage(pageNum, size, wrapper, orderNo, movieId, cinemaId, status, startDate, endDate);
        }

        LambdaQueryWrapper<TicketOrder> wrapper = new LambdaQueryWrapper<>();
        return buildPage(pageNum, size, wrapper, orderNo, movieId, cinemaId, status, startDate, endDate);
    }

    private OrderPageVO buildPage(int pageNum, int size, LambdaQueryWrapper<TicketOrder> wrapper,
                                   String orderNo, Long movieId, Long cinemaId, String status,
                                   String startDate, String endDate) {
        if (StringUtils.hasText(orderNo)) {
            wrapper.like(TicketOrder::getOrderNo, orderNo);//按照订单号进行模糊查询
        }
        if (movieId != null) {
            // 通过 showtime 表找 movieId 匹配的场次
            List<Showtime> showtimes = showtimeMapper.selectList(
                    new LambdaQueryWrapper<Showtime>().eq(Showtime::getMovieId, movieId));
            List<Long> showtimeIds = showtimes.stream().map(Showtime::getId).toList();
            if (showtimeIds.isEmpty()) {
                OrderPageVO empty = new OrderPageVO();
                empty.setTotal(0); empty.setPage(pageNum); empty.setSize(size);
                empty.setRecords(new ArrayList<>());
                return empty;
            }
            wrapper.in(TicketOrder::getShowtimeId, showtimeIds);
        }
        if (cinemaId != null) {
            // 通过 hall → cinema 关联
            List<Hall> halls = hallMapper.selectList(new LambdaQueryWrapper<Hall>().eq(Hall::getCinemaId, cinemaId));
            List<Long> hallIds = halls.stream().map(Hall::getId).toList();
            List<Showtime> showtimes = showtimeMapper.selectList(
                    new LambdaQueryWrapper<Showtime>().in(Showtime::getHallId, hallIds));
            List<Long> showtimeIds = showtimes.stream().map(Showtime::getId).toList();
            if (showtimeIds.isEmpty()) {
                OrderPageVO empty = new OrderPageVO();
                empty.setTotal(0); empty.setPage(pageNum); empty.setSize(size);
                empty.setRecords(new ArrayList<>());
                return empty;
            }
            wrapper.in(TicketOrder::getShowtimeId, showtimeIds);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(TicketOrder::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(TicketOrder::getCreateTime, LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(TicketOrder::getCreateTime, LocalDateTime.parse(endDate + "T23:59:59"));
        }
        wrapper.orderByDesc(TicketOrder::getCreateTime);

        Page<TicketOrder> pageResult = page(new Page<>(pageNum, size), wrapper);
        List<OrderVO> records = buildOrderVOList(pageResult.getRecords());

        OrderPageVO pageVO = new OrderPageVO();
        pageVO.setTotal(pageResult.getTotal());
        pageVO.setPage(pageNum);
        pageVO.setSize(size);
        pageVO.setRecords(records);
        return pageVO;
    }

    @Override
    public OrderDetailVO getOrderDetail(Long id) {
        TicketOrder order = getById(id);
        if (order == null) {
            throw new OrderException(ErrorCode.ORDER_NOT_FOUND);
        }

        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setAmount(AmountUtil.yuan(order.getAmount()));
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatusUtil.statusDesc(order.getStatus()));
        vo.setExpiresAt(order.getExpiresAt());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());

        // 用户
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            vo.setUserEmail(user.getEmail());
        }

        // 场次 → 影片 / 影厅 / 影院
        Showtime showtime = showtimeMapper.selectById(order.getShowtimeId());
        if (showtime != null) {
            vo.setStartAt(showtime.getStartAt());
            vo.setEndAt(showtime.getEndAt());
            vo.setLanguage(showtime.getLanguage());

            Movie movie = movieMapper.selectById(showtime.getMovieId());
            if (movie != null) {
                vo.setMovieName(movie.getName());
                vo.setMoviePoster(movie.getPoster());
            }

            Hall hall = hallMapper.selectById(showtime.getHallId());
            if (hall != null) {
                vo.setHallName(hall.getName());
                vo.setHallType(hall.getHallType() != null ? hall.getHallType().getDesc() : null);

                Cinema cinema = cinemaMapper.selectById(hall.getCinemaId());
                if (cinema != null) {
                    vo.setCinemaName(cinema.getName());
                    vo.setCinemaAddress(cinema.getAddress());
                }
            }
        }

        // 订单明细
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        List<OrderDetailVO.OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : items) {
            OrderDetailVO.OrderItemVO itemVO = new OrderDetailVO.OrderItemVO();
            itemVO.setUnitPrice(AmountUtil.yuan(item.getUnitPrice()));

            ShowtimeSeat showtimeSeat = showtimeSeatMapper.selectById(item.getSeatId());
            if (showtimeSeat != null) {
                Seat seat = seatMapper.selectById(showtimeSeat.getSeatId());
                if (seat != null) {
                    itemVO.setRowNo(seat.getRowNo());
                    itemVO.setSeatNo(seat.getSeatNo());
                    itemVO.setZone(seat.getZone());
                }
            }

            // 关联的电子票
            List<com.szml.movieticket.entity.Ticket> tickets = ticketMapper.selectList(
                    new LambdaQueryWrapper<com.szml.movieticket.entity.Ticket>()
                            .eq(com.szml.movieticket.entity.Ticket::getOrderItemId, item.getId()));
            if (!tickets.isEmpty()) {
                itemVO.setTicketCode(tickets.get(0).getTicketCode());
            }

            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);

        // 支付
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, id));
        if (payment != null) {
            OrderDetailVO.PaymentInfoVO paymentVO = new OrderDetailVO.PaymentInfoVO();
            paymentVO.setStatus(payment.getStatus());
            paymentVO.setAmount(AmountUtil.yuan(payment.getAmount()));
            paymentVO.setIdempotencyKey(payment.getIdempotencyKey());
            paymentVO.setProcessedAt(payment.getProcessedAt());
            vo.setPayment(paymentVO);
        }

        // 电子票
        List<com.szml.movieticket.entity.Ticket> allTickets = ticketMapper.selectList(
                new LambdaQueryWrapper<com.szml.movieticket.entity.Ticket>()
                        .eq(com.szml.movieticket.entity.Ticket::getOrderId, id));
        List<OrderDetailVO.TicketInfoVO> ticketVOs = new ArrayList<>();
        for (com.szml.movieticket.entity.Ticket ticket : allTickets) {
            OrderDetailVO.TicketInfoVO ticketVO = new OrderDetailVO.TicketInfoVO();
            ticketVO.setTicketCode(ticket.getTicketCode());
            ticketVO.setQrContent(ticket.getQrContent());
            // 通过 order_item 找到座位
            OrderItem orderItem = orderItemMapper.selectById(ticket.getOrderItemId());
            if (orderItem != null) {
                ShowtimeSeat sts = showtimeSeatMapper.selectById(orderItem.getSeatId());
                if (sts != null) {
                    Seat seat = seatMapper.selectById(sts.getSeatId());
                    if (seat != null) {
                        ticketVO.setRowNo(seat.getRowNo());
                        ticketVO.setSeatNo(seat.getSeatNo());
                    }
                }
            }
            ticketVOs.add(ticketVO);
        }
        vo.setTickets(ticketVOs);

        // 锁座审计日志
        List<SeatLockLog> logs = seatLockLogMapper.selectList(
                new LambdaQueryWrapper<SeatLockLog>().eq(SeatLockLog::getOrderId, id));
        List<OrderDetailVO.LockLogVO> logVOs = new ArrayList<>();
        for (SeatLockLog log : logs) {
            OrderDetailVO.LockLogVO logVO = new OrderDetailVO.LockLogVO();
            logVO.setAction(log.getAction());
            logVO.setSeatId(log.getSeatId());
            logVO.setCreateTime(log.getCreateTime());
            logVOs.add(logVO);
        }
        vo.setSeatLockLogs(logVOs);

        return vo;
    }

    /**
     * 批量构建订单 VO，避免 N+1 循环查库。
     */
    private List<OrderVO> buildOrderVOList(List<TicketOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }

        // 收集 IDs
        Set<Long> userIds = new HashSet<>();
        Set<Long> showtimeIds = new HashSet<>();
        Set<Long> orderIds = new HashSet<>();
        for (TicketOrder o : orders) {
            userIds.add(o.getUserId());
            showtimeIds.add(o.getShowtimeId());
            orderIds.add(o.getId());
        }

        // 1. 批量查 User
        Map<Long, User> userMap = new HashMap<>();
        for (User u : userMapper.selectBatchIds(userIds)) {
            userMap.put(u.getId(), u);
        }

        // 2. 批量查 Showtime
        Map<Long, Showtime> showtimeMap = new HashMap<>();
        for (Showtime s : showtimeMapper.selectBatchIds(showtimeIds)) {
            showtimeMap.put(s.getId(), s);
        }

        // 3. 从 Showtime 收集 movieIds, hallIds
        Set<Long> movieIds = new HashSet<>();
        Set<Long> hallIds = new HashSet<>();
        for (Showtime s : showtimeMap.values()) {
            movieIds.add(s.getMovieId());
            hallIds.add(s.getHallId());
        }

        // 4. 批量查 Movie
        Map<Long, Movie> movieMap = new HashMap<>();
        if (!movieIds.isEmpty()) {
            for (Movie m : movieMapper.selectBatchIds(movieIds)) {
                movieMap.put(m.getId(), m);
            }
        }

        // 5. 批量查 Hall
        Map<Long, Hall> hallMap = new HashMap<>();
        if (!hallIds.isEmpty()) {
            for (Hall h : hallMapper.selectBatchIds(hallIds)) {
                hallMap.put(h.getId(), h);
            }
        }

        // 6. 从 Hall 收集 cinemaIds
        Set<Long> cinemaIds = new HashSet<>();
        for (Hall h : hallMap.values()) {
            cinemaIds.add(h.getCinemaId());
        }

        // 7. 批量查 Cinema
        Map<Long, Cinema> cinemaMap = new HashMap<>();
        if (!cinemaIds.isEmpty()) {
            for (Cinema c : cinemaMapper.selectBatchIds(cinemaIds)) {
                cinemaMap.put(c.getId(), c);
            }
        }

        // 8. 批量查 OrderItem
        Map<Long, List<OrderItem>> itemsByOrderId = new HashMap<>();
        Set<Long> allSeatIds = new HashSet<>();
        for (OrderItem item : orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds))) {
            itemsByOrderId.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
            allSeatIds.add(item.getSeatId());
        }

        // 9. 批量查 ShowtimeSeat
        Map<Long, ShowtimeSeat> showtimeSeatMap = new HashMap<>();
        if (!allSeatIds.isEmpty()) {
            for (ShowtimeSeat sts : showtimeSeatMapper.selectBatchIds(allSeatIds)) {
                showtimeSeatMap.put(sts.getId(), sts);
            }
        }

        // 10. 从 ShowtimeSeat 收集物理座位 ID
        Set<Long> physicalSeatIds = new HashSet<>();
        for (ShowtimeSeat sts : showtimeSeatMap.values()) {
            physicalSeatIds.add(sts.getSeatId());
        }

        // 11. 批量查物理 Seat
        Map<Long, Seat> seatMap = new HashMap<>();
        if (!physicalSeatIds.isEmpty()) {
            for (Seat s : seatMapper.selectBatchIds(physicalSeatIds)) {
                seatMap.put(s.getId(), s);
            }
        }

        // 12. 组装
        return orders.stream().map(order -> {
            OrderVO vo = new OrderVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setUserId(order.getUserId());
            vo.setAmount(AmountUtil.yuan(order.getAmount()));
            vo.setStatus(order.getStatus());
            vo.setStatusDesc(OrderStatusUtil.statusDesc(order.getStatus()));
            vo.setCreateTime(order.getCreateTime());

            User user = userMap.get(order.getUserId());
            if (user != null) {
                vo.setUserEmail(user.getEmail());
            }

            Showtime showtime = showtimeMap.get(order.getShowtimeId());
            if (showtime != null) {
                vo.setStartAt(showtime.getStartAt());

                Movie movie = movieMap.get(showtime.getMovieId());
                if (movie != null) {
                    vo.setMovieName(movie.getName());
                }

                Hall hall = hallMap.get(showtime.getHallId());
                if (hall != null) {
                    vo.setHallName(hall.getName());

                    Cinema cinema = cinemaMap.get(hall.getCinemaId());
                    if (cinema != null) {
                        vo.setCinemaName(cinema.getName());
                    }
                }
            }

            List<OrderItem> items = itemsByOrderId.getOrDefault(order.getId(), List.of());
            List<String> seatLabels = new ArrayList<>();
            for (OrderItem item : items) {
                ShowtimeSeat sts = showtimeSeatMap.get(item.getSeatId());
                if (sts != null) {
                    Seat seat = seatMap.get(sts.getSeatId());
                    if (seat != null) {
                        seatLabels.add(seat.getRowNo() + "排" + seat.getSeatNo() + "座");
                    }
                }
            }
            vo.setSeatSummary(String.join(", ", seatLabels));
            return vo;
        }).collect(Collectors.toList());
    }

    private OrderVO toVO(TicketOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setAmount(AmountUtil.yuan(order.getAmount()));
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(OrderStatusUtil.statusDesc(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());

        // 用户邮箱
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            vo.setUserEmail(user.getEmail());
        }

        // 场次信息
        Showtime showtime = showtimeMapper.selectById(order.getShowtimeId());
        if (showtime != null) {
            vo.setStartAt(showtime.getStartAt());

            Movie movie = movieMapper.selectById(showtime.getMovieId());
            if (movie != null) {
                vo.setMovieName(movie.getName());
            }

            Hall hall = hallMapper.selectById(showtime.getHallId());
            if (hall != null) {
                vo.setHallName(hall.getName());

                Cinema cinema = cinemaMapper.selectById(hall.getCinemaId());
                if (cinema != null) {
                    vo.setCinemaName(cinema.getName());
                }
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
    }

}
