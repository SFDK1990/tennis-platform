package com.tennisplatform.identity.application.port.in;

import java.time.Instant;

/**
 * What a successful login or refresh produces. The raw refresh token appears here so the web
 * adapter can put it in a Set-Cookie header; it must never reach a response body.
 */
public record AuthenticationResult(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UserSummary user) {
}
