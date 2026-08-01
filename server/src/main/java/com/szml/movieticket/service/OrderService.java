package com.szml.movieticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szml.movieticket.entity.TicketOrder;
import com.szml.movieticket.vo.OrderDetailVO;
import com.szml.movieticket.vo.OrderPageVO;

/**
 * 订单服务接口（B 端只读）。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
public interface OrderService extends IService<TicketOrder> {

    /**
     * 分页查询订单列表。
     */
    OrderPageVO pageOrders(int page, int size, String orderNo, String email,
                           Long movieId, Long cinemaId, String status,
                           String startDate, String endDate);

    /**
     * 查询订单详情。
     */
    OrderDetailVO getOrderDetail(Long id);
}
