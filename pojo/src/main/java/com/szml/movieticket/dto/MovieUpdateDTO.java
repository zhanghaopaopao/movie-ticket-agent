package com.szml.movieticket.dto;

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

    private String name;

    private String genre;

    private Integer duration;

    private BigDecimal rating;

    private String poster;

    private String description;

    private String cast;

    private LocalDate releaseDate;
}
