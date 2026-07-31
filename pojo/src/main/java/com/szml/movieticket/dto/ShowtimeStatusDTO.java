package com.szml.movieticket.dto;

import com.szml.movieticket.enums.ShowtimeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 场次停售 DTO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class ShowtimeStatusDTO {

    @NotNull(message = "目标状态不能为空")
    private ShowtimeStatus status;
}
