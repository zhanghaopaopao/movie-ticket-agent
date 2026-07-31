package com.szml.movieticket.exception;

import com.szml.movieticket.enumeration.ErrorCode;

/**
 * 影院业务异常。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
public class CinemaException extends BusinessException {

    public CinemaException(ErrorCode errorCode) {
        super(errorCode);
    }
}
