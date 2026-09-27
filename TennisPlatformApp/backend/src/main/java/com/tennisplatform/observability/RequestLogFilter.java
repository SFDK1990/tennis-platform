package com.tennisplatform.observability;

import com.tennisplatform.error.ProblemCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * One line per API request: method, route, status, error code and duration. It is what is looked
 * for first when somebody says "it gave me an error" (28-fase16-analisis-observabilidad.md).
 *
 * <p>Inside {@link com.tennisplatform.web.CorrelationIdFilter}, so the line carries the correlation id, and outside the
 * security chain, so it also covers the requests security turns away. Who made the request comes
 * from the MDC, where the authentication filter puts it; this filter takes it out again, because
 * the thread goes back to the pool with whatever the MDC still holds.
 *
 * <p>The route is the controller's pattern, {@code /api/v1/lessons/{id}/bookings}, not the path
 * with its ids, and the query string is never written.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RequestLogFilter extends OncePerRequestFilter {

    public static final String MDC_USER_ID = "userId";
    public static final String MDC_ROLE = "role";

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            if (request.getRequestURI().startsWith("/api/")) {
                log.info("{} {} {} {} {} ms", request.getMethod(), routeOf(request), response.getStatus(),
                        ProblemCode.of(request), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            }
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_ROLE);
        }
    }

    /** A request security turned away never reached a controller, and so has no pattern. */
    private static String routeOf(HttpServletRequest request) {
        return request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) instanceof String pattern
                ? pattern
                : request.getRequestURI();
    }
}
