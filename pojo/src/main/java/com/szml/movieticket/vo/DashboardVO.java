package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/**
 * 数据看板 VO。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
@Data
public class DashboardVO {

    /** 今日统计 */
    private TodayStatsVO todayStats;

    /** 座位销售统计 */
    private SeatStatsVO seatStats;

    /** 热门影片 Top 5 */
    private List<TopMovieVO> topMovies;

    /** 近 7 天每日订单汇总 */
    private List<DailyOrderVO> last7DaysOrders;

    @Data
    public static class TodayStatsVO {
        /** 今日新增用户 */
        private long newUsers;
        /** 今日订单数 */
        private long orderCount;
        /** 今日已支付订单数 */
        private long paidOrderCount;
        /** 今日成交额（元） */
        private double revenue;
        /** 支付转化率 */
        private double conversionRate;
    }

    @Data
    public static class SeatStatsVO {
        private double soldRate;
        private long totalSold;
        private long totalAvailable;
    }

    @Data
    public static class TopMovieVO {
        private Long id;
        private String name;
        private long orderCount;
        private double revenue;
    }

    @Data
    public static class DailyOrderVO {
        private String date;
        private long count;
        private double revenue;
    }
}
