package com.szml.movieticket.exception;

import com.szml.movieticket.enumeration.ErrorCode;

/**
 * 认证异常，用于登录、注册、Token 校验等认证场景。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
public class AuthException extends BusinessException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
