package com.szml.movieticket.exception;

import com.szml.movieticket.enumeration.ErrorCode;

/**
 * 草稿业务异常。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
public class DraftException extends BusinessException {

    public DraftException(ErrorCode errorCode) {
        super(errorCode);
    }
}
