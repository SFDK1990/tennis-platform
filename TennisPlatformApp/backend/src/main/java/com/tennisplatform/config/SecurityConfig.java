package com.tennisplatform.config;

import com.tennisplatform.identity.adapters.in.web.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/actuator/health", "/actuator/health/**", "/actuator/info", "/error"
    };

    /** Reachable without a token by definition: these are how you obtain one. */
    private static final String[] PUBLIC_AUTH_PATHS = {
        "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh",
        "/api/v1/auth/logout", "/api/v1/auth/verify-email",
        "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
            // CSRF only where a cookie alone can authenticate the request. Everything else is
            // authorized by a bearer token, which a cross-site form cannot attach.
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .requireCsrfProtectionMatcher(new OrRequestMatcher(
                        new AntPathRequestMatcher("/api/v1/auth/refresh", "POST"),
                        new AntPathRequestMatcher("/api/v1/auth/logout", "POST"))))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers(PUBLIC_AUTH_PATHS).permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(handling -> handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new CsrfCookieFilter(), JwtAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Touching the token is what makes Spring Security actually write the XSRF-TOKEN cookie.
     * Without this the cookie is only sent on requests that already needed it, and the client
     * would have nothing to echo back on its first refresh.
     */
    static class CsrfCookieFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }

    /**
     * Placeholder with zero users. Spring Boot generates a random default account when no
     * UserDetailsService exists, printing its password on every startup; authentication here
     * is token based, so this exists only to keep that from happening.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
