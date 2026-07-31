package com.szml.movieticket.dto;

import com.szml.movieticket.enums.HallType;
import lombok.Data;

/**
 * 影厅编辑 DTO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class HallUpdateDTO {

    private String name;

    private HallType hallType;
}
