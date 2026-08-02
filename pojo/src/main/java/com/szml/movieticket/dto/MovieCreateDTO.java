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

    /** 影片名称 */
    @NotBlank(message = "影片名称不能为空")
    private String name;

    /** 影片类型，逗号分隔，如"科幻,冒险" */
    @NotBlank(message = "影片类型不能为空")
    private String genre;

    /** 时长（分钟） */
    @NotNull(message = "时长不能为空")
    private Integer duration;

    /** 评分，如 8.4 */
    private BigDecimal rating;

    /** 海报图片URL */
    private String poster;

    /** 上映状态 */
    @NotNull(message = "状态不能为空")
    private MovieStatus status;

    /** 影片简介 */
    private String description;

    /** 主演，逗号分隔 */
    private String cast;

    /** 上映日期 */
    @NotNull(message = "上映日期不能为空")
    private LocalDate releaseDate;
}
