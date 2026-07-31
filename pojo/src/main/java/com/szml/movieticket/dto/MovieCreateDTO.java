package com.szml.movieticket.dto;

import com.szml.movieticket.enums.MovieStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 影片新增 DTO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class MovieCreateDTO {

    @NotBlank(message = "影片名称不能为空")
    private String name;

    @NotBlank(message = "影片类型不能为空")
    private String genre;

    @NotNull(message = "时长不能为空")
    private Integer duration;

    private BigDecimal rating;

    private String poster;

    @NotNull(message = "状态不能为空")
    private MovieStatus status;

    private String description;

    private String cast;

    @NotNull(message = "上映日期不能为空")
    private LocalDate releaseDate;
}
