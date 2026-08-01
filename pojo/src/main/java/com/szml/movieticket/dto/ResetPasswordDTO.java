package com.szml.movieticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 找回密码 DTO。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
@Data
public class ResetPasswordDTO {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{8,32}$", message = "密码需8-32位且包含字母和数字")
    private String newPassword;
}
