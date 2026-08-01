package com.szml.movieticket.controller.admin;

import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.OrderService;
import com.szml.movieticket.vo.OrderDetailVO;
import com.szml.movieticket.vo.OrderPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单管理 Controller（B 端）。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 分页查询订单列表。
     */
    @GetMapping
    public Result<OrderPageVO> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String orderNo,
                                     @RequestParam(required = false) String email,
                                     @RequestParam(required = false) Long movieId,
                                     @RequestParam(required = false) Long cinemaId,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String startDate,
                                     @RequestParam(required = false) String endDate) {
        log.info("B端查询订单列表, 页码: {}, 每页条数: {}, 订单号: {}, 用户邮箱: {}, 影片ID: {}, 影院ID: {}, 订单状态: {}, 开始日期: {}, 结束日期: {}",
                page, size, orderNo, email, movieId, cinemaId, status, startDate, endDate);
        OrderPageVO orderPageVO = orderService.pageOrders(page, size, orderNo, email, movieId, cinemaId, status, startDate, endDate);
        return Result.success(orderPageVO);
    }

    /**
     * 订单详情。
     */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        log.info("查询订单详情, 订单ID: {}", id);
        OrderDetailVO orderDetailVO = orderService.getOrderDetail(id);
        return Result.success(orderDetailVO);
    }
}
