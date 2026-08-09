package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szml.movieticket.enums.MovieStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 影片实体。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
@TableName("movie")
public class Movie {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 影片名称 */
    private String name;

    /** 类型，逗号分隔 */
    private String genre;

    /** 时长（分钟） */
    private Integer duration;

    /** 评分 */
    private BigDecimal rating;

    /** 海报URL */
    private String poster;

    /** 状态 */
    private MovieStatus status;

    /** 简介 */
    private String description;

    /** 主演 */
    private String cast;

    /** 上映日期 */
    private LocalDate releaseDate;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除：0=正常 1=已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
