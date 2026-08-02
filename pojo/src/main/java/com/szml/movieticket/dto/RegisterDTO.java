package com.szml.movieticket.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册 DTO。
 *
 * @author zhanghao
 * @since 2026-08-01
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{8,32}$", message = "密码需8-32位且包含字母和数字")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String code;

//    @AssertTrue(message = "必须同意用户协议")
//    private Boolean agreeAgreement;
}
