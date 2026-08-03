package com.szml.movieticket.controller.admin;

import com.szml.movieticket.dto.LoginDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.AuthService;
import com.szml.movieticket.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * B端管理员认证接口。
 *
 * @author zhanghao
 * @since 2026-08-03
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    /**
     * B端管理员登录，非管理员角色直接拒绝。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("B端管理员登录, 手机号: {}", loginDTO.getPhone());
        LoginVO loginVO = authService.adminLogin(loginDTO.getPhone(), loginDTO.getPassword());
        return Result.success(loginVO);
    }
}
