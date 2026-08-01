package com.szml.movieticket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 影院 VO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class CinemaVO {

    private Long id;

    private String name;

    private String address;

    private String district;

    private String brand;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Integer status;

    private String statusDesc;

    private List<String> services;

    /** 最低票价（元） */
    private Double minPrice;

    /** 距离（km） */
    private Double distance;

    /** 该影院的厅型列表 */
    private List<String> hallTypes;

    /** 影厅数量 */
    private Integer hallCount;

    /** 当前在售场次数 */
    private Integer showtimeCount;

    private LocalDateTime createTime;
}
