package com.szml.movieticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 一种零食的选择项。 */
@Data
public class OrderSnackItemDTO {

    @NotNull
    private Long snackId;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer quantity;
}
