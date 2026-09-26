package com.tennisplatform.identity.adapters.in.web;

import com.tennisplatform.identity.configuration.IdentityProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles the authentication endpoints per client IP.
 *
 * <p>In memory on purpose: a single instance needs no shared state, and adding Redis now would
 * be complexity without a problem to solve. The day there are several instances this is the
 * one class to replace - which is why the counting lives here and nowhere else.
 *
 * <p>Known limit: per-IP counting does not stop a distributed attack against one account.
 * Account lockout is explicitly out of the MVP (02-arquitectura.md).
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String PROTECTED_PREFIX = "/api/v1/auth/";

    /** Bounds memory: an attacker rotating IPs must not be able to grow the map forever. */
    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;

    public AuthRateLimitFilter(IdentityProperties properties) {
        this.requestsPerMinute = properties.getAuthRateLimitPerMinute();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PROTECTED_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (bucketFor(clientIp(request)).tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, "60");
        response.getWriter().write("""
                {"title":"Too many requests","status":429,\
                "detail":"Too many attempts. Please wait a minute and try again.",\
                "code":"AUTH_RATE_LIMITED"}""");
    }

    private Bucket bucketFor(String clientIp) {
        if (buckets.size() > MAX_TRACKED_CLIENTS) {
            buckets.clear();
        }
        return buckets.computeIfAbsent(clientIp, ip -> Bucket.builder()
                // Intervally, not greedy: "20 per minute" must mean the allowance is
                // restored as a block once the minute is up, not trickled back one token at a
                // time - otherwise a slow, patient attacker is never actually throttled.
                .addLimit(Bandwidth.builder()
                        .capacity(requestsPerMinute)
                        .refillIntervally(requestsPerMinute, Duration.ofMinutes(1))
                        .build())
                .build());
    }

    /**
     * X-Forwarded-For is only trustworthy behind a proxy that overwrites it. Until the
     * deployment topology is decided, the socket address is the honest answer.
     */
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
