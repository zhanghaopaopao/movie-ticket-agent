package com.szml.movieticket.exception;

import com.szml.movieticket.enumeration.ErrorCode;

/**
 * 座位业务异常。
 */
public class SeatException extends BusinessException {

    public SeatException(ErrorCode errorCode) {
        super(errorCode);
    }
}
