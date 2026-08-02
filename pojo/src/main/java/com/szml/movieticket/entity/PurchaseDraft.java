package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购票草稿实体。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
@TableName("purchase_draft")
public class PurchaseDraft {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 已选影片ID */
    private Long movieId;

    /** 已选影院ID */
    private Long cinemaId;

    /** 日期时间偏好 JSON */
    private String dateTimeJson;

    /** 已选场次ID */
    private Long showtimeId;

    /** 票数（1-6），默认1 */
    private Integer ticketCount;

    /** 预算 JSON */
    private String budgetJson;

    /** 已选座位 JSON */
    private String seatsJson;

    /** 来源模式：AI / TRADITIONAL */
    private String sourceMode;

    /** 草稿状态：ACTIVE / FROZEN / ARCHIVED */
    private String status;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 关联订单ID（FROZEN 时有值） */
    private Long orderId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
