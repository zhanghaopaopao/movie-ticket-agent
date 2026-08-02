package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/**
 * C 端订单分页响应 VO。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
public class UserOrderPageVO {

    private long total;

    private int page;

    private int size;

    private List<UserOrderVO> records;
}
