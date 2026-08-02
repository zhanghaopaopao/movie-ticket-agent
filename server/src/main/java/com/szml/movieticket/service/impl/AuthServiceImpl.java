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
import com.szml.movieticket.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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

    private static final int TOKEN_TTL_MINUTES = 30;
    private static final int CODE_LENGTH = 6;
    private static final long CODE_TTL_SECONDS = 600;
    private static final long CODE_RATE_LIMIT_SECONDS = 60;
    private static final String REDIS_KEY_PREFIX = "email_code:";
    private static final String REDIS_LOGIN_FAIL_PREFIX = "login_fail:";
    private static final String REDIS_AUTH_PREFIX = "auth:";
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final int LOCK_MINUTES = 15;

    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final JavaMailSender mailSender;

    @Override
    public LoginVO login(String phone, String password) {
        log.info("用户登录请求, phone: {}", phone);

        // 检查 Redis 失败计数器（防暴力破解）
        String failKey = REDIS_LOGIN_FAIL_PREFIX + phone;
        String failCountStr = stringRedisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) : 0;
        if (failCount >= MAX_LOGIN_FAIL_COUNT) {
            log.warn("登录失败，账号已锁定(Redis), phone: {}, failCount: {}", phone, failCount);
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }

        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (user == null) {
            log.warn("登录失败，账号不存在, phone: {}", phone);
            incrementFailCount(failKey);
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND);
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("登录失败，账号已禁用, userId: {}", user.getId());
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("登录失败，密码错误, userId: {}", user.getId());
            int newFailCount = incrementFailCount(failKey);
            if (newFailCount >= MAX_LOGIN_FAIL_COUNT) {
                log.warn("登录失败已达{}次，账号已锁定(Redis), phone: {}", MAX_LOGIN_FAIL_COUNT, phone);
                throw new AuthException(ErrorCode.AUTH_ACCOUNT_LOCKED);
            }
            throw new AuthException(ErrorCode.AUTH_WRONG_PASSWORD);
        }

        // 登录成功：清除失败计数
        stringRedisTemplate.delete(failKey);

        // 生成会话令牌，存入 Redis（每次请求刷新 TTL）
        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(
                REDIS_AUTH_PREFIX + token,
                user.getId() + ":" + user.getRole().getCode(),
                Duration.ofMinutes(TOKEN_TTL_MINUTES));

        Map<String, Object> userInfo = new LinkedHashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("phone", user.getPhone());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole().getCode());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(userInfo);

        log.info("用户登录成功, userId: {}, role: {}", user.getId(), user.getRole());
        return loginVO;
    }

    @Override
    public void sendEmailCode(String email, Integer purpose) {
        String redisKey = REDIS_KEY_PREFIX + email + ":" + purpose;

        String existing = stringRedisTemplate.opsForValue().get(redisKey);
        if (existing != null) {
            Long remaining = stringRedisTemplate.getExpire(redisKey);//获取指定key的过期时间
            long elapsed = CODE_TTL_SECONDS - remaining;
            if (elapsed < CODE_RATE_LIMIT_SECONDS) {
                log.warn("验证码发送频率超限, email: {}, purpose: {}", email, purpose);
                throw new BusinessException(ErrorCode.EMAIL_CODE_RATE_LIMIT);
            }
        }

        String code = generateCode();
        String codeHash = sha256(code);
        stringRedisTemplate.opsForValue().set(redisKey, codeHash, Duration.ofSeconds(CODE_TTL_SECONDS));

        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("2571761868@qq.com");
            message.setTo(email);
            message.setSubject("电影票智能体 - 验证码");
            message.setText("您的验证码是：" + code + "，10分钟内有效，请勿泄露。");
            mailSender.send(message);
            log.info("验证码邮件已发送, email: {}, purpose: {}", email, purpose == 0 ? "注册" : "找回密码");
        } catch (Exception e) {
            log.error("验证码邮件发送失败, email: {}", email, e);
            // 发送失败但验证码已存 Redis，开发环境可从日志查看
            log.info("========== 验证码(邮件发送失败兜底) ==========");
            log.info("邮箱: {}, 用途: {}, 验证码: {}", email, purpose == 0 ? "注册" : "找回密码", code);
            log.info("==========================================");
        }
    }

    @Override
    public void register(String phone, String email, String password, String code) {
        String redisKey = REDIS_KEY_PREFIX + email + ":0";
        String storedHash = stringRedisTemplate.opsForValue().get(redisKey);//从redis中获取验证码
        if (storedHash == null || !storedHash.equals(sha256(code))) {//解析验证码
            throw new AuthException(ErrorCode.EMAIL_CODE_INVALID);
        }

        // 检查手机号、邮箱是否已存在
        long phoneCount = count(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (phoneCount > 0) {
            throw new BusinessException(ErrorCode.USER_PHONE_EXISTS);
        }

        long emailCount = count(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (emailCount > 0) {
            throw new BusinessException(ErrorCode.USER_EMAIL_EXISTS);
        }

        User user = new User();
        user.setPhone(phone);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        save(user);

        stringRedisTemplate.delete(redisKey);

        log.info("用户注册成功, userId: {}, email: {}", user.getId(), email);
    }

    @Override
    public void resetPassword(String email, String code, String newPassword) {
        String redisKey = REDIS_KEY_PREFIX + email + ":1";
        String storedHash = stringRedisTemplate.opsForValue().get(redisKey);
        if (storedHash == null || !storedHash.equals(sha256(code))) {
            throw new AuthException(ErrorCode.EMAIL_CODE_INVALID);
        }

        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AuthException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        updateById(user);

        stringRedisTemplate.delete(redisKey);

        log.info("密码重置成功, userId: {}, email: {}", user.getId(), email);
    }

    @Override
    public void logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        stringRedisTemplate.delete(REDIS_AUTH_PREFIX + token);
        log.info("用户退出登录成功");
    }

    private int incrementFailCount(String failKey) {
        Long newCount = stringRedisTemplate.opsForValue().increment(failKey);
        stringRedisTemplate.expire(failKey, Duration.ofMinutes(LOCK_MINUTES));
        return newCount != null ? newCount.intValue() : 0;
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
