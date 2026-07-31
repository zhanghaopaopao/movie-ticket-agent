package com.szml.movieticket.exception;

import com.szml.movieticket.enumeration.ErrorCode;

/**
 * 影片业务异常。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
public class MovieException extends BusinessException {

    public MovieException(ErrorCode errorCode) {
        super(errorCode);
    }
}
