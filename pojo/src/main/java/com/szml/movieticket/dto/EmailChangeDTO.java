package com.szml.movieticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EmailChangeDTO {
    @NotBlank(message = "当前邮箱验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "当前邮箱验证码格式不正确")
    private String currentEmailCode;

    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "新邮箱格式不正确")
    private String newEmail;

    @NotBlank(message = "新邮箱验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "新邮箱验证码格式不正确")
    private String newEmailCode;
}
