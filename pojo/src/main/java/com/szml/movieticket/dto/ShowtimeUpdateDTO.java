package com.szml.movieticket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场次编辑 DTO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class ShowtimeUpdateDTO {

    private LocalDateTime startAt;

    private Integer basePrice;

    private String language;
}
