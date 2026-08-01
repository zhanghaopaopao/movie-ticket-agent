package com.szml.movieticket.controller;

import com.szml.movieticket.result.Result;
import com.szml.movieticket.dto.LoginDTO;
import com.szml.movieticket.dto.RegisterDTO;
import com.szml.movieticket.dto.ResetPasswordDTO;
import com.szml.movieticket.dto.SendCodeDTO;
import com.szml.movieticket.vo.LoginVO;
import com.szml.movieticket.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（B/C 共用）。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 手机号 + 密码登录。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("用户登录, 手机号: {}", loginDTO.getPhone());
        LoginVO loginVO = authService.login(loginDTO.getPhone(), loginDTO.getPassword());
        return Result.success(loginVO);
    }

    /**
     * 发送邮箱验证码。
     */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO sendCodeDTO) {
        log.info("发送验证码, 邮箱: {}, 用途: {}", sendCodeDTO.getEmail(), sendCodeDTO.getPurpose());
        authService.sendEmailCode(sendCodeDTO.getEmail(), sendCodeDTO.getPurpose());
        return Result.success();
    }

    /**
     * 用户注册。
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        log.info("用户注册, 邮箱: {}, 手机号: {}", registerDTO.getEmail(), registerDTO.getPhone());
        authService.register(registerDTO.getPhone(), registerDTO.getEmail(),
                registerDTO.getPassword(), registerDTO.getCode());
        return Result.success();
    }

    /**
     * 找回密码。
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        log.info("找回密码, 邮箱: {}", resetPasswordDTO.getEmail());
        authService.resetPassword(resetPasswordDTO.getEmail(), resetPasswordDTO.getCode(),
                resetPasswordDTO.getNewPassword());
        return Result.success();
    }
}
