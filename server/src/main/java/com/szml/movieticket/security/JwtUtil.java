package com.szml.movieticket.security;

import com.szml.movieticket.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类，单令牌模式。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
@Component
public class
JwtUtil {

    private static final String ISSUER = "movie-ticket-agent";
    private static final long EXPIRATION_MS = 30 * 60 * 1000L;

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret:movie-ticket-agent-dev-secret-key-256-bit-minimum}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 JWT，有效期 30 分钟。
     */
    public String generateToken(Long userId, UserRole role) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(userId))
                .claim("role", role.getCode())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + EXPIRATION_MS))
                .signWith(secretKey)
    }

    /**
     * 解析并校验 Token。
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Claims 中提取用户 ID。
     */
    public Long getUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从 Claims 中提取角色。
     */
    public UserRole getRole(Claims claims) {
        return UserRole.fromCode(claims.get("role", Integer.class));
    }
}
