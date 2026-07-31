package com.szml.movieticket.vo;

import lombok.Data;

import java.util.Map;

/**
 * 登录响应 VO。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
@Data
public class LoginVO {

    /** JWT 令牌 */
    private String token;

    /** 有效期（秒） */
    private Integer expiresIn;

    /** 用户基本信息 */
    private Map<String, Object> user;
}
