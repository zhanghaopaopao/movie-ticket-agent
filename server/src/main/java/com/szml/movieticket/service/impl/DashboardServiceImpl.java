package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.szml.movieticket.entity.*;
import com.szml.movieticket.mapper.*;
import com.szml.movieticket.service.DashboardService;
import com.szml.movieticket.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据看板服务实现类。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final ShowtimeMapper showtimeMapper;
    private final MovieMapper movieMapper;
    private final ShowtimeSeatMapper showtimeSeatMapper;

    @Override
    public DashboardVO getDashboard() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        DashboardVO dashboard = new DashboardVO();
        dashboard.setTodayStats(buildTodayStats(todayStart, todayEnd));
        dashboard.setSeatStats(buildSeatStats());
        dashboard.setTopMovies(buildTopMovies(todayStart, todayEnd));
        dashboard.setLast7DaysOrders(buildLast7DaysOrders());
        return dashboard;
    }

    private DashboardVO.TodayStatsVO buildTodayStats(LocalDateTime todayStart, LocalDateTime todayEnd) {
        DashboardVO.TodayStatsVO stats = new DashboardVO.TodayStatsVO();

        // 今日新增用户
        stats.setNewUsers(userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreateTime, todayStart)));

        // 今日订单数
        stats.setOrderCount(orderMapper.selectCount(
                new LambdaQueryWrapper<TicketOrder>()
                        .ge(TicketOrder::getCreateTime, todayStart)
                        .lt(TicketOrder::getCreateTime, todayEnd)));

        // 今日已支付订单数（PAID + TICKETED）
        stats.setPaidOrderCount(orderMapper.selectCount(
                new LambdaQueryWrapper<TicketOrder>()
                        .ge(TicketOrder::getCreateTime, todayStart)
                        .lt(TicketOrder::getCreateTime, todayEnd)
                        .in(TicketOrder::getStatus, List.of("PAID", "TICKETED"))));

        // 今日成交额
        List<TicketOrder> paidOrders = orderMapper.selectList(
                new LambdaQueryWrapper<TicketOrder>()
                        .ge(TicketOrder::getCreateTime, todayStart)
                        .lt(TicketOrder::getCreateTime, todayEnd)
                        .in(TicketOrder::getStatus, List.of("PAID", "TICKETED")));
        long revenueCents = paidOrders.stream().mapToLong(o -> o.getAmount() != null ? o.getAmount() : 0).sum();
        stats.setRevenue(yuan(revenueCents));

        // 转化率
        long totalOrders = stats.getOrderCount();
        if (totalOrders > 0) {
            stats.setConversionRate(Math.round(stats.getPaidOrderCount() * 100.0 / totalOrders) / 100.0);
        }

        return stats;
    }

    private DashboardVO.SeatStatsVO buildSeatStats() {
        DashboardVO.SeatStatsVO stats = new DashboardVO.SeatStatsVO();
        long totalSold = showtimeSeatMapper.selectCount(
                new LambdaQueryWrapper<ShowtimeSeat>().eq(ShowtimeSeat::getStatus, 2));
        long totalAvailable = showtimeSeatMapper.selectCount(
                new LambdaQueryWrapper<ShowtimeSeat>().eq(ShowtimeSeat::getStatus, 0));
        long total = showtimeSeatMapper.selectCount(null);

        stats.setTotalSold(totalSold);
        stats.setTotalAvailable(totalAvailable);
        if (total > 0) {
            stats.setSoldRate(Math.round(totalSold * 100.0 / total) / 100.0);
        }
        return stats;
    }

    private List<DashboardVO.TopMovieVO> buildTopMovies(LocalDateTime todayStart, LocalDateTime todayEnd) {
        // 今日所有订单对应的场次
        List<TicketOrder> todayOrders = orderMapper.selectList(
                new LambdaQueryWrapper<TicketOrder>()
                        .ge(TicketOrder::getCreateTime, todayStart)
                        .lt(TicketOrder::getCreateTime, todayEnd)
                        .in(TicketOrder::getStatus, List.of("PAID", "TICKETED")));

        // 按 movieId 聚合
        Map<Long, DashboardVO.TopMovieVO> movieStats = new LinkedHashMap<>();
        for (TicketOrder order : todayOrders) {
            Showtime showtime = showtimeMapper.selectById(order.getShowtimeId());
            if (showtime == null) continue;

            DashboardVO.TopMovieVO stat = movieStats.computeIfAbsent(showtime.getMovieId(), mid -> {
                DashboardVO.TopMovieVO m = new DashboardVO.TopMovieVO();
                m.setId(mid);
                Movie movie = movieMapper.selectById(mid);
                m.setName(movie != null ? movie.getName() : "未知");
                m.setOrderCount(0);
                m.setRevenue(0);
                return m;
            });
            stat.setOrderCount(stat.getOrderCount() + 1);
            stat.setRevenue(stat.getRevenue() + yuan(order.getAmount() != null ? order.getAmount() : 0));
        }

        // 按订单数降序取 Top 5
        return movieStats.values().stream()
                .sorted((a, b) -> Long.compare(b.getOrderCount(), a.getOrderCount()))
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<DashboardVO.DailyOrderVO> buildLast7DaysOrders() {
        List<DashboardVO.DailyOrderVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            List<TicketOrder> dayOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<TicketOrder>()
                            .ge(TicketOrder::getCreateTime, dayStart)
                            .lt(TicketOrder::getCreateTime, dayEnd)
                            .in(TicketOrder::getStatus, List.of("PAID", "TICKETED")));

            DashboardVO.DailyOrderVO dailyVO = new DashboardVO.DailyOrderVO();
            dailyVO.setDate(date.toString());
            dailyVO.setCount(dayOrders.size());
            long revenueCents = dayOrders.stream().mapToLong(o -> o.getAmount() != null ? o.getAmount() : 0).sum();
            dailyVO.setRevenue(yuan(revenueCents));
            result.add(dailyVO);
        }

        return result;
    }

    private static double yuan(long cents) {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
    }
}
