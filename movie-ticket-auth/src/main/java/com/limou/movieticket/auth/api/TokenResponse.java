package com.limou.movieticket.auth.api;

import java.time.OffsetDateTime;

public record TokenResponse(
        String tokenType,
        String accessToken,
        OffsetDateTime accessTokenExpiresAt,
        String refreshToken,
        OffsetDateTime refreshTokenExpiresAt
) {
}
