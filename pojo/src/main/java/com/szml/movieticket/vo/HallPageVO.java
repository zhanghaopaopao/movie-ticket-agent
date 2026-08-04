package com.szml.movieticket.vo;

import lombok.Data;

import java.util.List;

/**
 * 影厅分页响应 VO。
 *
 * @author zhanghao
 * @since 2026-08-03
 */
@Data
public class HallPageVO {

    /** 总条数 */
    private long total;

    /** 当前页码 */
    private int page;

    /** 每页条数 */
    private int size;

    /** 影厅列表 */
    private List<HallVO> records;
}
