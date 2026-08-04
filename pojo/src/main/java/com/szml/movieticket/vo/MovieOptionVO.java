package com.szml.movieticket.vo;

import lombok.Data;

/**
 * 影片选项 VO（仅 id + name，供下拉框使用）。
 *
 * @author zhanghao
 * @since 2026-08-04
 */
@Data
public class MovieOptionVO {
    private Long id;
    private String name;
}
