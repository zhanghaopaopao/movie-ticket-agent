package com.szml.movieticket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 影片 VO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class MovieVO {

    /** 影片ID */
    private Long id;

    /** 影片名称 */
    private String name;

    /** 影片类型，逗号分隔，如"科幻,冒险" */
    private String genre;

    /** 时长（分钟） */
    private Integer duration;

    /** 评分 */
    private BigDecimal rating;

    /** 海报图片URL */
    private String poster;

    /** 状态：0=待上映 1=热映中 2=已下架 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    /** 影片简介 */
    private String description;

    /** 主演，逗号分隔 */
    private String cast;

    /** 上映日期 */
    private LocalDate releaseDate;

    /** 当日在售场次数 */
    private Integer showtimeCount;

    /** 当日覆盖影院数 */
    private Integer cinemaCount;

    /** 当前登录用户是否已加入想看。 */
    private Boolean wanted;

    /** 创建时间 */
    private LocalDateTime createTime;
}
