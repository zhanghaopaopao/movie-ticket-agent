package com.szml.movieticket.dto;

import com.szml.movieticket.enums.MovieStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 影片上下架 DTO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class MovieStatusDTO {

    @NotNull(message = "目标状态不能为空")
    private MovieStatus status;
}
