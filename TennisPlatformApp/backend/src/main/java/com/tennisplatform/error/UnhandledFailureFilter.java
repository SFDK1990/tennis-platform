package com.tennisplatform.error;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Catches what fails outside the controllers - in a filter, before {@link GlobalExceptionHandler}
 * can see it - and answers the same 500. Left alone, the exception reached Tomcat, which logs it
 * with its message, and a message can quote personal data (28-fase16-analisis-observabilidad.md).
 *
 * <p>Inside the request log and the metrics filters, so the failure is logged and counted as
 * {@code 500 INTERNAL_ERROR} like any other; outside everything else.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
class UnhandledFailureFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UnhandledFailureFilter.class);

    private final ProblemDetailWriter writer;

    UnhandledFailureFilter(ProblemDetailWriter writer) {
        this.writer = writer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException {
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException failure) {
            log.error("Unhandled exception", WithoutMessage.of(failure));
            writer.write(request, response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
        }
    }
}
