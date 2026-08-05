package com.szml.movieticket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 新增影院零食商品。 */
@Data
public class SnackProductCreateDTO {

    @NotNull
    private Long cinemaId;

    @NotBlank
    private String name;

    private String description;

    private String image;

    @NotNull
    @Min(1)
    private Integer priceFen;

    @NotNull
    @Min(0)
    private Integer stock;
}
