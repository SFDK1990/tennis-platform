package com.tennisplatform.identity.adapters.in.web;

import com.tennisplatform.identity.configuration.IdentityProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

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

    /**
     * Bounds memory: an attacker rotating IPs must not be able to grow the map forever. Past
     * the bound the least recently seen client is evicted; wiping the whole map, as it used to,
     * handed that attacker a reset of everybody's counters.
     */
    private static final int MAX_TRACKED_CLIENTS = 50_000;

    /**
     * Only a literal address is ever compared: resolving a name the client wrote into the header
     * would make every request a DNS lookup of the attacker's choosing.
     */
    private static final Pattern IP_LITERAL = Pattern.compile("[0-9A-Fa-f.:]{2,45}");

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(MAX_TRACKED_CLIENTS)
            .expireAfterAccess(Duration.ofMinutes(2))
            .build();
    private final int requestsPerMinute;
    private final List<IpAddressMatcher> trustedProxies;

    public AuthRateLimitFilter(IdentityProperties properties) {
        this.requestsPerMinute = properties.getAuthRateLimitPerMinute();
        this.trustedProxies = properties.getTrustedProxies().stream().map(IpAddressMatcher::new).toList();
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
        return buckets.get(clientIp, ip -> Bucket.builder()
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
     * The browser never reaches the backend: it talks to Next, which forwards /api. The socket
     * address is therefore the frontend's for every user, and counting by it put all of them
     * in one bucket - 21 requests a minute from anybody locked everybody out of signing in.
     *
     * <p>So when the socket is a trusted proxy, the client is the right-most address in
     * X-Forwarded-For that is not itself a trusted proxy. Right-most, because each proxy appends
     * to the header and only what our own proxies wrote can be believed; anything to its left
     * came from the client. From anywhere else the header is ignored, or any caller could pick
     * a fresh "IP" per request.
     *
     * <p>Next fills the header only when it is absent, so it passes on one sent by the client.
     * That is why the proxy in front of it must overwrite the header (Fase 17).
     */
    String clientIp(HttpServletRequest request) {
        String socket = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || !isTrustedProxy(socket)) {
            return socket;
        }
        String[] hops = forwarded.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (!IP_LITERAL.matcher(hop).matches()) {
                return socket;
            }
            if (!isTrustedProxy(hop)) {
                return hop;
            }
        }
        return socket;
    }

    private boolean isTrustedProxy(String address) {
        try {
            return trustedProxies.stream().anyMatch(proxy -> proxy.matches(address));
        } catch (IllegalArgumentException notAnAddress) {
            return false;
        }
    }
}
