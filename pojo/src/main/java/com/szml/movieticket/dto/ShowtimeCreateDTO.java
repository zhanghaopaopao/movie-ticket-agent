package com.szml.movieticket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场次新增 DTO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class ShowtimeCreateDTO {

    @NotNull(message = "影片ID不能为空")
    private Long movieId;

    @NotNull(message = "影厅ID不能为空")
    private Long hallId;

    @NotNull(message = "开场时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startAt;

    @NotNull(message = "基准票价不能为空")
    private Integer basePrice;

    /** 语言格式，默认国语2D */
    private String language;
}
