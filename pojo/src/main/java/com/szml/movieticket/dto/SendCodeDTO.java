package com.szml.movieticket.dto;

import jakarta.validation.constraints.Email;
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

    /** 0=注册 1=找回密码 */
    @NotNull(message = "用途不能为空")
    private Integer purpose;
}
