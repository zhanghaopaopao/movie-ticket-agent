package com.szml.movieticket.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/** 修改零食商品的可编辑字段。 */
@Data
public class SnackProductUpdateDTO {

    private String name;

    private String description;

    private String image;

    @Min(1)
    private Integer priceFen;
}
