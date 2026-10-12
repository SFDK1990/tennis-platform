package com.tennisplatform.identity.adapters.in.web;

import com.tennisplatform.identity.configuration.IdentityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
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
 *
 * <p>Added straight to the servlet response, never as a {@code ResponseEntity} header: since
 * Spring Framework 7 those replace the Set-Cookie already on the response, and the one there
 * is the XSRF-TOKEN that {@code SecurityConfig} writes. The browser then signed in with no
 * CSRF cookie, and its first refresh got a 403.
 */
@Component
public class RefreshTokenCookie {

    public static final String NAME = "refresh_token";
    private static final String PATH = "/api/v1/auth";

    private final IdentityProperties properties;

    RefreshTokenCookie(IdentityProperties properties) {
        this.properties = properties;
    }

    public void issue(HttpServletResponse response, String rawToken, Duration maxAge) {
        add(response, base(rawToken).maxAge(maxAge).build());
    }

    /** An already expired, empty cookie: the standard way to delete one. */
    public void clear(HttpServletResponse response) {
        add(response, base("").maxAge(Duration.ZERO).build());
    }

    private static void add(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
