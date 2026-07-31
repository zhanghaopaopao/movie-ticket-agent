package com.szml.movieticket.dto;

import com.szml.movieticket.enums.CinemaStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 影院启停 DTO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class CinemaStatusDTO {

    @NotNull(message = "目标状态不能为空")
    private CinemaStatus status;
}
