package com.szml.movieticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量保存影厅座位布局 DTO。
 */
@Data
public class SeatLayoutSaveDTO {

    @NotNull(message = "座位布局不能为空")
    @Valid
    private List<SeatLayoutItemDTO> seats;
}
