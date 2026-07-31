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

    private Long id;

    private String name;

    private String genre;

    private Integer duration;

    private BigDecimal rating;

    private String poster;

    private Integer status;

    private String statusDesc;

    private String description;

    private String cast;

    private LocalDate releaseDate;

    /** 当前在售场次数 */
    private Integer showtimeCount;

    private LocalDateTime createTime;
}
