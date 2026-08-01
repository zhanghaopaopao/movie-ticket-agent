package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 座位锁定审计日志实体。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
@TableName("seat_lock_log")
public class SeatLockLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 场次ID */
    private Long showtimeId;

    /** 座位ID（showtime_seat.id） */
    private Long seatId;

    /** 操作类型：LOCK / RELEASE / EXPIRE */
    private String action;

    private LocalDateTime createTime;
}
