package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户观影偏好实体。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
@TableName("user_preference")
public class UserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 常用商圈 */
    private String district;

    /** 偏好厅型 */
    private String hallType;

    /** 单票预算上限（分） */
    private Integer budget;

    /** 偏好座位区域 */
    private String seatZone;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
