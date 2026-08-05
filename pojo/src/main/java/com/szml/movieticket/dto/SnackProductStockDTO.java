package com.szml.movieticket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 设置零食可售库存。 */
@Data
public class SnackProductStockDTO {

    @NotNull
    @Min(0)
    private Integer stock;
}
