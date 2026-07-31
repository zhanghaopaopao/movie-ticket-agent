package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szml.movieticket.enums.ShowtimeStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场次实体。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
@TableName("showtime")
public class Showtime {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 影片ID */
    private Long movieId;

    /** 影厅ID */
    private Long hallId;

    /** 开场时间 */
    private LocalDateTime startAt;

    /** 散场时间 = startAt + movie.duration + 10min */
    private LocalDateTime endAt;

    /** 基准票价（分） */
    private Integer basePrice;

    /** 语言格式 */
    private String language;

    /** 状态 */
    private ShowtimeStatus status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
