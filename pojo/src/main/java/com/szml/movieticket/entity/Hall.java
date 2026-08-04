package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szml.movieticket.enums.HallStatus;
import com.szml.movieticket.enums.HallType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 影厅实体。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
@TableName("hall")
public class Hall {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属影院ID */
    private Long cinemaId;

    /** 影厅名称 */
    private String name;

    /** 厅型 */
    private HallType hallType;

    /** 状态：0=停用 1=启用 */
    private HallStatus status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
