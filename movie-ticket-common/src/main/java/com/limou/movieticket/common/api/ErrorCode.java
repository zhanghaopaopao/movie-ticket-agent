package com.limou.movieticket.common.api;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    OK("OK", "success", HttpStatus.OK),
    VALIDATION_FAILED("VALIDATION_FAILED", "Request validation failed", HttpStatus.BAD_REQUEST),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "Email or password is incorrect", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", "Authentication token is invalid", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_REVOKED("AUTH_TOKEN_REVOKED", "Refresh token has been revoked", HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_DISABLED("AUTH_ACCOUNT_DISABLED", "Account is disabled", HttpStatus.FORBIDDEN),
    AUTH_ACCOUNT_LOCKED("AUTH_ACCOUNT_LOCKED", "Account is temporarily locked", HttpStatus.LOCKED),
    ACCESS_DENIED("ACCESS_DENIED", "Access denied", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Requested resource was not found", HttpStatus.NOT_FOUND),
    PURCHASE_DRAFT_CONFLICT("PURCHASE_DRAFT_CONFLICT", "Purchase draft version conflict", HttpStatus.CONFLICT),
    PURCHASE_DRAFT_FROZEN("PURCHASE_DRAFT_FROZEN", "Purchase draft is frozen", HttpStatus.CONFLICT),
    SEAT_NOT_AVAILABLE("SEAT_NOT_AVAILABLE", "Seat is not available", HttpStatus.CONFLICT),
    ORDER_EXPIRED("ORDER_EXPIRED", "Order has expired", HttpStatus.CONFLICT),
    ORDER_STATE_CONFLICT("ORDER_STATE_CONFLICT", "Order state does not allow this operation", HttpStatus.CONFLICT),
    PAYMENT_ALREADY_PROCESSED("PAYMENT_ALREADY_PROCESSED", "Payment has already been processed", HttpStatus.CONFLICT),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
