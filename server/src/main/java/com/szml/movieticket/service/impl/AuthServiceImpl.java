package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.AuthException;
import com.szml.movieticket.exception.BusinessException;
import com.szml.movieticket.entity.User;
import com.szml.movieticket.enums.UserRole;
import com.szml.movieticket.enums.UserStatus;
import com.szml.movieticket.vo.LoginVO;
import com.szml.movieticket.mapper.UserMapper;
import com.szml.movieticket.security.JwtUtil;
import com.szml.movieticket.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
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

    private static final int TOKEN_EXPIRES_IN = 1800;
    private static final int CODE_LENGTH = 6;
    private static final long CODE_TTL_SECONDS = 600;
    private static final long CODE_RATE_LIMIT_SECONDS = 60;
    private static final String REDIS_KEY_PREFIX = "email_code:";

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

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

        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("phone", user.getPhone());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole().getCode());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setExpiresIn(TOKEN_EXPIRES_IN);
        loginVO.setUser(userInfo);

        log.info("用户登录成功, userId: {}, role: {}", user.getId(), user.getRole());
        return loginVO;
    }

    @Override
    public void sendEmailCode(String email, Integer purpose) {
        String redisKey = REDIS_KEY_PREFIX + email + ":" + purpose;

        // 60s 限流
        String existing = stringRedisTemplate.opsForValue().get(redisKey);
        if (existing != null) {
            Long remaining = stringRedisTemplate.getExpire(redisKey);
            long elapsed = CODE_TTL_SECONDS - (remaining != null ? remaining : 0);
            if (elapsed < CODE_RATE_LIMIT_SECONDS) {
                log.warn("验证码发送频率超限, email: {}, purpose: {}", email, purpose);
                throw new BusinessException(ErrorCode.EMAIL_CODE_RATE_LIMIT);
            }
        }

        // 生成验证码
        String code = generateCode();

        // 存入 Redis，10min 有效期
        stringRedisTemplate.opsForValue().set(redisKey, code, Duration.ofSeconds(CODE_TTL_SECONDS));

        // 开发环境打印验证码到日志
        log.info("========== 验证码 ==========");
        log.info("邮箱: {}, 用途: {}, 验证码: {}", email, purpose == 0 ? "注册" : "找回密码", code);
        log.info("============================");

        // TODO: 生产环境通过 SMTP 发送邮件
    }

    @Override
    public void register(String phone, String email, String password, String code) {
        // 校验验证码
        String redisKey = REDIS_KEY_PREFIX + email + ":0";
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (storedCode == null || !storedCode.equals(code)) {
            throw new AuthException(ErrorCode.EMAIL_CODE_INVALID);
        }

        // 手机号唯一性
        long phoneCount = count(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (phoneCount > 0) {
            throw new BusinessException(ErrorCode.USER_PHONE_EXISTS);
        }

        // 邮箱唯一性
        long emailCount = count(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (emailCount > 0) {
            throw new BusinessException(ErrorCode.USER_EMAIL_EXISTS);
        }

        // 创建用户，role 固定 USER
        User user = new User();
        user.setPhone(phone);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        save(user);

        // 删除已使用的验证码
        stringRedisTemplate.delete(redisKey);

        log.info("用户注册成功, userId: {}, email: {}", user.getId(), email);
    }

    @Override
    public void resetPassword(String email, String code, String newPassword) {
        // 校验验证码
        String redisKey = REDIS_KEY_PREFIX + email + ":1";
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (storedCode == null || !storedCode.equals(code)) {
            throw new AuthException(ErrorCode.EMAIL_CODE_INVALID);
        }

        // 根据邮箱查用户
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND);
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        updateById(user);

        // 删除已使用的验证码
        stringRedisTemplate.delete(redisKey);

        log.info("密码重置成功, userId: {}, email: {}", user.getId(), email);
    }

    /**
     * 生成 6 位数字验证码。
     */
    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
