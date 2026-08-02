package com.szml.movieticket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量布局中的一个座位。id 为空表示新增，已有 id 表示更新，未出现在列表中的旧座位会被删除。
 */
@Data
public class SeatLayoutItemDTO {

    private Long id;

    @NotNull(message = "排号不能为空")
    @Min(value = 1, message = "排号必须大于0")
    private Integer rowNo;

    @NotNull(message = "座号不能为空")
    @Min(value = 1, message = "座号必须大于0")
    private Integer seatNo;

    @NotBlank(message = "座位区域不能为空")
    private String zone;

    @NotNull(message = "座位类型不能为空")
    private Integer seatType;

    private Integer status;
}
