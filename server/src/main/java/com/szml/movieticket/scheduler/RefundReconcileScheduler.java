package com.szml.movieticket.scheduler;

import com.szml.movieticket.service.OrderRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每分钟主动查询支付宝，收敛网络异常导致的退款处理中订单。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundReconcileScheduler {

    private final OrderRefundService orderRefundService;

    @Scheduled(cron = "0 * * * * *")
    public void reconcilePendingRefunds() {
        orderRefundService.reconcilePendingRefunds();
    }
}
