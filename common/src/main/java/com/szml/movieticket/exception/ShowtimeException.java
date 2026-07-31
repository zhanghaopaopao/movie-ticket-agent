package com.szml.movieticket.exception;

import com.szml.movieticket.enumeration.ErrorCode;

/**
 * 场次业务异常。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
public class ShowtimeException extends BusinessException {

    public ShowtimeException(ErrorCode errorCode) {
        super(errorCode);
    }
}
