package com.szml.movieticket.controller.admin;

import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.DashboardService;
import com.szml.movieticket.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据看板 Controller（B 端）。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 数据看板。
     */
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        log.info("查询数据看板");
        DashboardVO dashboardVO = dashboardService.getDashboard();
        return Result.success(dashboardVO);
    }
}
