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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务实现类（B 端只读）。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, TicketOrder> implements OrderService {

    private final UserMapper userMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final com.szml.movieticket.mapper.TicketMapper ticketMapper;
    private final SeatLockLogMapper seatLockLogMapper;
    private final ShowtimeMapper showtimeMapper;
    private final MovieMapper movieMapper;
    private final HallMapper hallMapper;
    private final CinemaMapper cinemaMapper;
    private final ShowtimeSeatMapper showtimeSeatMapper;
    private final SeatMapper seatMapper;

    public OrderServiceImpl(UserMapper userMapper, OrderItemMapper orderItemMapper,
                            PaymentMapper paymentMapper, com.szml.movieticket.mapper.TicketMapper ticketMapper,
                            SeatLockLogMapper seatLockLogMapper, ShowtimeMapper showtimeMapper,
                            MovieMapper movieMapper, HallMapper hallMapper, CinemaMapper cinemaMapper,
                            ShowtimeSeatMapper showtimeSeatMapper, SeatMapper seatMapper) {
        this.userMapper = userMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentMapper = paymentMapper;
        this.ticketMapper = ticketMapper;
        this.seatLockLogMapper = seatLockLogMapper;
        this.showtimeMapper = showtimeMapper;
        this.movieMapper = movieMapper;
        this.hallMapper = hallMapper;
        this.cinemaMapper = cinemaMapper;
        this.showtimeSeatMapper = showtimeSeatMapper;
        this.seatMapper = seatMapper;
    }

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
            wrapper.eq(TicketOrder::getOrderNo, orderNo);
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
        List<OrderVO> records = pageResult.getRecords().stream().map(this::toVO).collect(Collectors.toList());

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
        vo.setAmount(yuan(order.getAmount()));
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(statusDesc(order.getStatus()));
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
            itemVO.setUnitPrice(yuan(item.getUnitPrice()));

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
            paymentVO.setAmount(yuan(payment.getAmount()));
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

    private OrderVO toVO(TicketOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setAmount(yuan(order.getAmount()));
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(statusDesc(order.getStatus()));
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

    private static Double yuan(int cents) {
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
