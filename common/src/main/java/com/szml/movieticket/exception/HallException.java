package com.szml.movieticket.exception;

import com.szml.movieticket.enumeration.ErrorCode;

/**
 * 影厅业务异常。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
public class HallException extends BusinessException {

    public HallException(ErrorCode errorCode) {
        super(errorCode);
    }
}
