package com.szml.movieticket.dto;

import com.szml.movieticket.enums.HallType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 影厅新增 DTO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class HallCreateDTO {

    @NotNull(message = "影院ID不能为空")
    private Long cinemaId;

    @NotBlank(message = "影厅名称不能为空")
    private String name;

    @NotNull(message = "厅型不能为空")
    private HallType hallType;
}
