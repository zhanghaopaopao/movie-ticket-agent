package com.szml.movieticket.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.common.enumeration.ErrorCode;
import com.szml.movieticket.common.exception.AuthException;
import com.szml.movieticket.pojo.entity.User;
import com.szml.movieticket.pojo.enums.UserStatus;
import com.szml.movieticket.pojo.vo.LoginVO;
import com.szml.movieticket.server.mapper.UserMapper;
import com.szml.movieticket.server.security.JwtUtil;
import com.szml.movieticket.server.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证服务实现类。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<UserMapper, User> implements AuthService {

    private static final int ACCESS_TOKEN_EXPIRES_IN = 1800;

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(String phone, String password) {
        log.info("用户登录请求, phone: {}", phone);

        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            log.warn("登录失败，账号不存在, phone: {}", phone);
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND);
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("登录失败，账号已禁用, userId: {}", user.getId());
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
            log.warn("登录失败，账号已锁定, userId: {}", user.getId());
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("登录失败，密码错误, userId: {}", user.getId());
            throw new AuthException(ErrorCode.AUTH_WRONG_PASSWORD);
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("phone", user.getPhone());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole().getCode());

        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setExpiresIn(ACCESS_TOKEN_EXPIRES_IN);
        loginVO.setUser(userInfo);

        log.info("用户登录成功, userId: {}, role: {}", user.getId(), user.getRole());
        return loginVO;
    }
}
