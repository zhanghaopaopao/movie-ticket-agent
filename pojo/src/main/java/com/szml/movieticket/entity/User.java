package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szml.movieticket.enums.UserRole;
import com.szml.movieticket.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表实体。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像 URL */
    private String avatarUrl;

    /** BCrypt 密码摘要 */
    private String passwordHash;

    /** 角色 */
    private UserRole role;

    /** 状态 */
    private UserStatus status;

    /** 锁定到期时间 */
    private LocalDateTime lockedUntil;

    /** 注册时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
