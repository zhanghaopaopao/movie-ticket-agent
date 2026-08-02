package com.szml.movieticket.controller.user;

import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.dto.LockSeatsDTO;
import com.szml.movieticket.dto.PayDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.OrderTicketService;
import com.szml.movieticket.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单与支付接口（C 端）。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Slf4j
@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderTicketService orderTicketService;

    /**
     * 锁座。
     */
    @PostMapping("/lock")
    public Result<LockResultVO> lock(@Valid @RequestBody LockSeatsDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("锁座请求, 用户ID: {}, 场次ID: {}, 座位数量: {}", userId, dto.getShowtimeId(), dto.getSeatIds().size());
        LockResultVO lockResultVO = orderTicketService.lockSeats(userId, dto.getShowtimeId(), dto.getSeatIds(), dto.getDraftVersion());
        return Result.success(lockResultVO);
    }

    /**
     * 模拟支付。
     */
    @PostMapping("/{id}/pay")
    public Result<PayResultVO> pay(@PathVariable Long id, @Valid @RequestBody PayDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("支付请求, 用户ID: {}, 订单ID: {}", userId, id);
        PayResultVO payResultVO = orderTicketService.pay(userId, id, dto.getIdempotencyKey());
        return Result.success(payResultVO);
    }

    /**
     * 取消订单。
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        log.info("取消订单, 用户ID: {}, 订单ID: {}", userId, id);
        orderTicketService.cancelOrder(userId, id);
        return Result.success();
    }

    /**
     * 订单列表。
     */
    @GetMapping
    public Result<UserOrderPageVO> list(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) String status) {
        Long userId = UserContext.getUserId();
        log.info("查询订单列表, 用户ID: {}, 订单状态: {}", userId, status);
        UserOrderPageVO userOrderPageVO = orderTicketService.listOrders(userId, page, size, status);
        return Result.success(userOrderPageVO);
    }

    /**
     * 订单详情。
     */
    @GetMapping("/{id}")
    public Result<UserOrderDetailVO> detail(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        log.info("查询订单详情, 用户ID: {}, 订单ID: {}", userId, id);
        UserOrderDetailVO userOrderDetailVO = orderTicketService.getOrderDetail(userId, id);
        return Result.success(userOrderDetailVO);
    }
}
