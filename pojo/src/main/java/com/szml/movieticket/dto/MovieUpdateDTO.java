package com.szml.movieticket.dto;

import com.szml.movieticket.enums.MovieStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 影片编辑 DTO，仅更新非 null 字段。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class MovieUpdateDTO {

    /** 影片名称 */
    private String name;

    /** 影片类型，逗号分隔，如"科幻,冒险" */
    private String genre;

    /** 时长（分钟） */
    private Integer duration;

    /** 评分，如 8.4 */
    private BigDecimal rating;

    /** 海报图片URL */
    private String poster;

    /** 影片简介 */
    private String description;

    /** 主演，逗号分隔 */
    private String cast;

    /** 上映日期 */
    private LocalDate releaseDate;

    /** 上映状态 */
    private MovieStatus status;
}
