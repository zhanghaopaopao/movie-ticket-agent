package com.szml.movieticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 设置零食商品上下架状态。 */
@Data
public class SnackProductStatusDTO {

    @NotNull
    private Integer status;
}
