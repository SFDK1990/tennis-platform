package com.tennisplatform.identity.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.identity.adapters.out.security.JwtAccessTokens;
import com.tennisplatform.observability.RequestLogFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Turns a valid bearer token into an authenticated request.
 *
 * <p>Never rejects by itself: an absent or invalid token simply leaves the request
 * unauthenticated and the authorization rules decide. That keeps public endpoints working and
 * concentrates the "who may do what" decision in one place.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtAccessTokens accessTokens;

    JwtAuthenticationFilter(JwtAccessTokens accessTokens) {
        this.accessTokens = accessTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        bearerToken(request)
                .flatMap(accessTokens::verify)
                .ifPresent(this::authenticate);

        chain.doFilter(request, response);
    }

    private void authenticate(AuthenticatedUser user) {
        // The principal is the token's own claim set. Downstream code must take the caller's
        // identity from here and never from a path variable or a request body field.
        var authentication = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // Ids and a role, never the email: every log line of the request says who made it
        // without saying who they are. RequestLogFilter takes them out when the request ends.
        MDC.put(RequestLogFilter.MDC_USER_ID, user.id().toString());
        MDC.put(RequestLogFilter.MDC_ROLE, user.role().name());
    }

    private java.util.Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return java.util.Optional.empty();
        }
        String value = header.substring(BEARER_PREFIX.length()).trim();
        return value.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(value);
    }
}
