package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/**
 * 场次分页响应 VO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class ShowtimePageVO {

    private long total;

    private int page;

    private int size;

    private List<ShowtimeVO> records;
}
