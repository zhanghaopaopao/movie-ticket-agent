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

    /** 影院ID */
    private Long id;

    /** 影院名称 */
    private String name;

    /** 详细地址 */
    private String address;

    /** 所属区域 */
    private String district;

    /** 品牌 */
    private String brand;

    /** 纬度 */
    private BigDecimal latitude;

    /** 经度 */
    private BigDecimal longitude;

    /** 状态：0=停用 1=营业中 */
    private Integer status;

    /** 状态描述 */
    private String statusDesc;

    /** 支持的服务列表，如"退票,改签" */
    private List<String> services;

    /** 最低票价（元） */
    private Double minPrice;

    /** 距离（km），仅 C 端按位置查询时返回 */
    private Double distance;

    /** 该影院的厅型列表 */
    private List<String> hallTypes;

    /** 影厅数量 */
    private Integer hallCount;

    /** 当前在售场次数 */
    private Integer showtimeCount;

    /** 创建时间 */
    private LocalDateTime createTime;
}
