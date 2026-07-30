package com.szml.movieticket.pojo.vo;

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

    private String accessToken;

    private String refreshToken;

    /** 有效期（秒） */
    private Integer expiresIn;

    /** 用户基本信息 */
    private Map<String, Object> user;
}
