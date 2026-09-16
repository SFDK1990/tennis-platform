package com.tennisplatform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    /**
     * An incoming correlation id is attacker-controlled input that ends up in two dangerous
     * places: a response header and every log line of the request. Anything outside this set
     * is discarded rather than sanitised - a client with an exotic id loses nothing, since a
     * fresh one is generated for it.
     *
     * <p>Rejecting control characters blocks log forging (injecting fake log lines through
     * CR/LF) and response header splitting. The length cap stops a single request from
     * pushing an 8 KB header into every log line it produces.
     */
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER_NAME);
        String correlationId = isSafe(incoming) ? incoming : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static boolean isSafe(String incoming) {
        return StringUtils.hasText(incoming) && SAFE_CORRELATION_ID.matcher(incoming).matches();
    }
}
