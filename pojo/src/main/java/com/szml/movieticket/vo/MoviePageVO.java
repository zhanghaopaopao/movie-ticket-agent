package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/**
 * 影片分页响应 VO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class MoviePageVO {

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int page;

    /** 每页条数 */
    private int size;

    /** 影片列表 */
    private List<MovieVO> records;
}
