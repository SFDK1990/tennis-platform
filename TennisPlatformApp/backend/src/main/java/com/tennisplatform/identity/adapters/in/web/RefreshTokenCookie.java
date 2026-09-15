package com.tennisplatform.identity.adapters.in.web;

import com.tennisplatform.identity.configuration.IdentityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Builds and reads the refresh token cookie.
 *
 * <p>HttpOnly so no script can read it, SameSite=Strict so browsers never attach it to
 * cross-site requests, and scoped to the auth path so it is not sent on every API call the
 * application makes - a token that travels less is a token that leaks less.
 */
@Component
public class RefreshTokenCookie {

    public static final String NAME = "refresh_token";
    private static final String PATH = "/api/v1/auth";

    private final IdentityProperties properties;

    RefreshTokenCookie(IdentityProperties properties) {
        this.properties = properties;
    }

    public String issue(String rawToken, Duration maxAge) {
        return base(rawToken).maxAge(maxAge).build().toString();
    }

    /** An already expired, empty cookie: the standard way to delete one. */
    public String clear() {
        return base("").maxAge(Duration.ZERO).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite("Strict")
                .path(PATH);
    }

    public Optional<String> readFrom(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> NAME.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }
}
