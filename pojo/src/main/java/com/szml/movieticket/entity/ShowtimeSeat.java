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

    /** 库存ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 场次ID */
    private Long showtimeId;

    /** 物理座位ID */
    private Long seatId;

    /** 该场次下该座位的售价（分），null 时使用场次基准价 */
    private Integer price;

    /** 状态：0=可选 1=已锁定 2=已售 3=不可用 4=情侣座 */
    private Integer status;

    /** 锁定者用户ID */
    private Long lockOwner;

    /** 锁座过期时间 */
    private LocalDateTime lockExpiresAt;

    /** 乐观锁版本号 */
    private Integer version;
}
