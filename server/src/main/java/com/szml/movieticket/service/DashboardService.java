package com.szml.movieticket.service;

import com.szml.movieticket.vo.DashboardVO;

/**
 * 数据看板服务接口。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
public interface DashboardService {

    /**
     * 获取数据看板汇总。
     */
    DashboardVO getDashboard();
}
