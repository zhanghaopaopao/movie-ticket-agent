package com.szml.movieticket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    /** 关联在售场次数 */
    private Integer showtimeCount;

    /** 关联覆盖影院数 */
    private Integer cinemaCount;

    /** 当前登录用户是否已加入想看。 */
    private Boolean wanted;

    /** 近期场次摘要（Agent 专用，每部影片最多返回几条，C 端不使用）。 */
    private List<ShowtimeSummaryVO> upcomingShowtimes;

    /** 创建时间 */
    private LocalDateTime createTime;
}
