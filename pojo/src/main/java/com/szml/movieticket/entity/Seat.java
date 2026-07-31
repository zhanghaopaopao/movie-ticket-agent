package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 物理座位实体。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
@TableName("seat")
public class Seat {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属影厅ID */
    private Long hallId;

    /** 排号 */
    private Integer rowNo;

    /** 座号 */
    private Integer seatNo;

    /** 区域：FRONT/MIDDLE/BACK/COUPLE */
    private String zone;

    /** 类型：0=普通座 1=情侣座 */
    private Integer seatType;

    /** 物理状态：0=可用 1=永久不可用 */
    private Integer status;
}
