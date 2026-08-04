package com.szml.movieticket.service;

/** Email verification code service. */
public interface EmailCodeService {
    int PURPOSE_REGISTER = 0;
    int PURPOSE_RESET_PASSWORD = 1;
    int PURPOSE_EMAIL_LOGIN = 2;
    int PURPOSE_ACCOUNT_SECURITY = 3;
    int PURPOSE_NEW_EMAIL = 4;

    void sendCode(String email, int purpose);

    /** 校验验证码但不消费，用于需要同时校验多个验证码的业务。 */
    void verifyCode(String email, int purpose, String code);

    void consumeCode(String email, int purpose, String code);
}
