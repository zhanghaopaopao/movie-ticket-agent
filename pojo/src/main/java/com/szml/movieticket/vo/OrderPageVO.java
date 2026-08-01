package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/**
 * 订单分页响应 VO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class OrderPageVO {

    private long total;

    private int page;

    private int size;

    private List<OrderVO> records;
}
