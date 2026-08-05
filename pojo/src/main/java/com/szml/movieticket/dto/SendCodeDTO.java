package com.szml.movieticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送验证码 DTO。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
@Data
public class SendCodeDTO {

    @Email(message = "邮箱格式不正确")
    @NotNull(message = "邮箱不能为空")
    private String email;

    /** 0=注册 1=找回密码 2=登录 */
    @NotNull(message = "用途不能为空")
    @Min(value = 0, message = "验证码用途不正确")
    @Max(value = 2, message = "账号安全验证码必须登录后发送")
    private Integer purpose;
}
