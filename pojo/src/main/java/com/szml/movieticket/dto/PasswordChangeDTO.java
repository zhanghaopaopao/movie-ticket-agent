package com.szml.movieticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改密码 DTO。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Data
public class PasswordChangeDTO {

    @NotBlank(message = "当前密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{8,32}$", message = "密码需8-32位且包含字母和数字")
    private String newPassword;
}
