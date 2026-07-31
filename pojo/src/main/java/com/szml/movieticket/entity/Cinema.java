package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szml.movieticket.enums.CinemaStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 影院实体。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
@TableName("cinema")
public class Cinema {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 影院名称 */
    private String name;

    /** 详细地址 */
    private String address;

    /** 所属商圈 */
    private String district;

    /** 品牌 */
    private String brand;

    /** 纬度 */
    private BigDecimal latitude;

    /** 经度 */
    private BigDecimal longitude;

    /** 状态 */
    private CinemaStatus status;

    /** 服务标签 JSON 数组 */
    private String services;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
