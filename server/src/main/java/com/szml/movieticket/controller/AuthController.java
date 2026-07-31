package com.szml.movieticket.controller;

import com.szml.movieticket.result.Result;
import com.szml.movieticket.dto.LoginDTO;
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
        log.info("收到登录请求, phone: {}", loginDTO.getPhone());
        LoginVO loginVO = authService.login(loginDTO.getPhone(), loginDTO.getPassword());
        return Result.success(loginVO);
    }
}
