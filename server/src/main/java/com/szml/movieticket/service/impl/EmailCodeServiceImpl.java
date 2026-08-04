package com.szml.movieticket.service.impl;

import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.AuthException;
import com.szml.movieticket.exception.BusinessException;
import com.szml.movieticket.service.EmailCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeServiceImpl implements EmailCodeService {
    private static final int CODE_LENGTH = 6;
    private static final long CODE_TTL_SECONDS = 600;
    private static final long CODE_RATE_LIMIT_SECONDS = 60;
    private static final String REDIS_KEY_PREFIX = "email_code:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Override
    public void sendCode(String email, int purpose) {
        validatePurpose(purpose);
        String redisKey = redisKey(email, purpose);
        if (stringRedisTemplate.opsForValue().get(redisKey) != null) {
            Long remaining = stringRedisTemplate.getExpire(redisKey);
            if (remaining != null && remaining >= 0 && CODE_TTL_SECONDS - remaining < CODE_RATE_LIMIT_SECONDS) {
                throw new BusinessException(ErrorCode.EMAIL_CODE_RATE_LIMIT);
            }
        }

        String code = generateCode();
        stringRedisTemplate.opsForValue().set(redisKey, sha256(code), Duration.ofSeconds(CODE_TTL_SECONDS));
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("电影票智能体 - 验证码");
            message.setText("您的验证码是：" + code + "，10分钟内有效，请勿泄露。用途：" + purposeDesc(purpose));
            mailSender.send(message);
            log.info("验证码邮件已发送, email: {}, purpose: {}", email, purposeDesc(purpose));
        } catch (Exception exception) {
            stringRedisTemplate.delete(redisKey);
            log.error("验证码邮件发送失败, email: {}, purpose: {}", email, purpose, exception);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    @Override
    public void verifyCode(String email, int purpose, String code) {
        validatePurpose(purpose);
        String redisKey = redisKey(email, purpose);
        String storedHash = stringRedisTemplate.opsForValue().get(redisKey);
        if (storedHash == null || code == null || !storedHash.equals(sha256(code))) {
            throw new AuthException(ErrorCode.EMAIL_CODE_INVALID);
        }
    }

    @Override
    public void consumeCode(String email, int purpose, String code) {
        verifyCode(email, purpose, code);
        String redisKey = redisKey(email, purpose);
        stringRedisTemplate.delete(redisKey);
    }

    private static void validatePurpose(int purpose) {
        if (purpose < PURPOSE_REGISTER || purpose > PURPOSE_NEW_EMAIL) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
    }

    private static String redisKey(String email, int purpose) {
        return REDIS_KEY_PREFIX + email + ":" + purpose;
    }

    private static String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) code.append(random.nextInt(10));
        return code.toString();
    }

    private static String purposeDesc(int purpose) {
        return switch (purpose) {
            case PURPOSE_REGISTER -> "注册";
            case PURPOSE_RESET_PASSWORD -> "找回密码";
            case PURPOSE_EMAIL_LOGIN -> "邮箱登录";
            case PURPOSE_ACCOUNT_SECURITY -> "账号安全验证";
            case PURPOSE_NEW_EMAIL -> "绑定新邮箱";
            default -> "未知";
        };
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : hash) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
