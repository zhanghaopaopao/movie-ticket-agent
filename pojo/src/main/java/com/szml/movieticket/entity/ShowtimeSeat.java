package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场次座位库存实体。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
@TableName("showtime_seat")
public class ShowtimeSeat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long showtimeId;

    private Long seatId;

    private Integer price;

    /** 0=可选 1=已锁定 2=已售 3=不可用 4=情侣座 */
    private Integer status;

    private Long lockOwner;

    private LocalDateTime lockExpiresAt;

    private Integer version;
}
