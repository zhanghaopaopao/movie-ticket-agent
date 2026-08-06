package com.szml.movieticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 整体替换待支付订单的零食选择。 */
@Data
public class OrderSnackSelectionDTO {

    @NotNull
    @Valid
    private List<OrderSnackItemDTO> items = new ArrayList<>();
}
