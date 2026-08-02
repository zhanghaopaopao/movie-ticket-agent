package com.szml.movieticket.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 场次座位状态批量设置 DTO。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
@Data
public class ShowtimeSeatStatusDTO {

    @NotEmpty(message = "座位ID列表不能为空")
    private List<Long> seatIds;

    /** AVAILABLE / UNAVAILABLE */
    @NotNull(message = "目标状态不能为空")
    @Pattern(regexp = "AVAILABLE|UNAVAILABLE", message = "目标状态必须为 AVAILABLE 或 UNAVAILABLE")
    private String status;
}
