package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户搜索历史实体。
 *
 * <p>userId 逻辑关联 user.id；现有数据库未启用外键约束，因此由服务层保证用户边界。</p>
 */
@Data
@TableName("user_search_history")
public class UserSearchHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String keyword;

    private Integer searchCount;

    private LocalDateTime lastSearchTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
