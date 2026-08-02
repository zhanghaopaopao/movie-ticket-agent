package com.szml.movieticket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增物理座位 DTO。
 */
@Data
public class SeatCreateDTO {

    @NotNull(message = "排号不能为空")
    @Min(value = 1, message = "排号必须大于0")
    private Integer rowNo;

    @NotNull(message = "座号不能为空")
    @Min(value = 1, message = "座号必须大于0")
    private Integer seatNo;

    @NotBlank(message = "座位区域不能为空")
    private String zone;

    /** 0=普通座，1=情侣座 */
    @NotNull(message = "座位类型不能为空")
    private Integer seatType;

    /** 0=可用，1=永久不可用 */
    private Integer status;
}
