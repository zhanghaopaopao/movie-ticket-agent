package com.limou.movieticket.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.limou.movieticket.auth.api.LoginRequest;
import com.limou.movieticket.auth.api.TokenResponse;
import com.limou.movieticket.auth.config.JwtProperties;
import com.limou.movieticket.auth.domain.AppUser;
import com.limou.movieticket.auth.domain.RefreshToken;
import com.limou.movieticket.auth.domain.UserStatus;
import com.limou.movieticket.auth.mapper.AppUserMapper;
import com.limou.movieticket.auth.mapper.RefreshTokenMapper;
import com.limou.movieticket.common.api.ErrorCode;
import com.limou.movieticket.common.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private static final int MAX_FAILURES = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AppUserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties properties;

    public AuthService(AppUserMapper userMapper, RefreshTokenMapper refreshTokenMapper,
                       PasswordEncoder passwordEncoder, JwtService jwtService, JwtProperties properties) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public TokenResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        AppUser user = userMapper.selectOne(Wrappers.<AppUser>lambdaQuery().eq(AppUser::getEmail, normalizedEmail));
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        ensureUserCanAuthenticate(user);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordFailure(user);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        clearFailures(user);
        return issueTokenPair(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenMapper.selectByHashForUpdate(sha256(rawRefreshToken));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (stored == null || stored.getRevokedAt() != null || !stored.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_REVOKED);
        }
        AppUser user = userMapper.selectById(stored.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        ensureUserCanAuthenticate(user);
        stored.setRevokedAt(now);
        refreshTokenMapper.updateById(stored);
        return issueTokenPair(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        RefreshToken stored = refreshTokenMapper.selectOne(Wrappers.<RefreshToken>lambdaQuery()
                .eq(RefreshToken::getTokenHash, sha256(rawRefreshToken)));
        if (stored != null && stored.getRevokedAt() == null) {
            stored.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
            refreshTokenMapper.updateById(stored);
        }
    }

    private TokenResponse issueTokenPair(AppUser user) {
        JwtService.EncodedAccessToken accessToken = jwtService.issueAccessToken(user);
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant refreshExpiresAt = Instant.now().plus(properties.refreshTtl());

        RefreshToken stored = new RefreshToken();
        stored.setId("rt_" + UUID.randomUUID().toString().replace("-", ""));
        stored.setUserId(user.getId());
        stored.setTokenHash(sha256(rawRefreshToken));
        stored.setExpiresAt(LocalDateTime.ofInstant(refreshExpiresAt, ZoneOffset.UTC));
        stored.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        refreshTokenMapper.insert(stored);

        return new TokenResponse("Bearer", accessToken.value(), OffsetDateTime.ofInstant(accessToken.expiresAt(), ZoneOffset.UTC),
                rawRefreshToken, OffsetDateTime.ofInstant(refreshExpiresAt, ZoneOffset.UTC));
    }

    private void ensureUserCanAuthenticate(AppUser user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED,
                    "Account is locked until " + user.getLockedUntil() + "Z");
        }
    }

    private void recordFailure(AppUser user) {
        int failures = user.getLoginFailureCount() == null ? 1 : user.getLoginFailureCount() + 1;
        user.setLoginFailureCount(failures);
        if (failures >= MAX_FAILURES) {
            user.setLockedUntil(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(15));
            user.setLoginFailureCount(0);
        }
        userMapper.updateById(user);
    }

    private void clearFailures(AppUser user) {
        if ((user.getLoginFailureCount() != null && user.getLoginFailureCount() != 0) || user.getLockedUntil() != null) {
            userMapper.update(null, Wrappers.<AppUser>lambdaUpdate()
                    .eq(AppUser::getId, user.getId())
                    .set(AppUser::getLoginFailureCount, 0)
                    .set(AppUser::getLockedUntil, null));
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
