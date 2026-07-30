package com.szml.movieticket.common.exception;

import com.szml.movieticket.common.enumeration.ErrorCode;
import lombok.Getter;

/**
 * 业务异常基类。
 * 消息由 ErrorCode 枚举自包含，各业务域创建子类继承。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
