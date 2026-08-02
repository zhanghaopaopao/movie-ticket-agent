package com.szml.movieticket.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 编辑物理座位 DTO。未传字段保持原值。
 */
@Data
public class SeatUpdateDTO {

    @Min(value = 1, message = "排号必须大于0")
    private Integer rowNo;

    @Min(value = 1, message = "座号必须大于0")
    private Integer seatNo;

    private String zone;

    /** 0=普通座，1=情侣座 */
    private Integer seatType;

    /** 0=可用，1=永久不可用 */
    private Integer status;
}
