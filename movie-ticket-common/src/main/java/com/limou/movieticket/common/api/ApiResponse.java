package com.limou.movieticket.common.api;

import com.limou.movieticket.common.trace.TraceContext;

public record ApiResponse<T>(String code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.OK.getCode(), ErrorCode.OK.getDefaultMessage(), data, TraceContext.getTraceId());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, T data) {
        String resolvedMessage = message == null || message.isBlank() ? errorCode.getDefaultMessage() : message;
        return new ApiResponse<>(errorCode.getCode(), resolvedMessage, data, TraceContext.getTraceId());
    }
}
