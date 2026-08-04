package com.szml.movieticket.dto;

import com.szml.movieticket.enums.HallStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 影厅启停 DTO。
 *
 * @author zhanghao
 * @since 2026-08-04
 */
@Data
public class HallStatusDTO {

    @NotNull(message = "目标状态不能为空")
    private HallStatus status;
}
