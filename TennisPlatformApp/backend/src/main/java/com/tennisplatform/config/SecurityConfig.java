package com.tennisplatform.config;

import com.tennisplatform.error.ProblemDetailAccessDeniedHandler;
import com.tennisplatform.error.ProblemDetailAuthenticationEntryPoint;
import com.tennisplatform.identity.adapters.in.web.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
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
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                           ProblemDetailAuthenticationEntryPoint entryPoint,
                                           ProblemDetailAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
            // CSRF only where a cookie alone can authenticate the request. Everything else is
            // authorized by a bearer token, which a cross-site form cannot attach.
            .csrf(csrf -> {
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .requireCsrfProtectionMatcher(new OrRequestMatcher(
                            new AntPathRequestMatcher("/api/v1/auth/refresh", "POST"),
                            new AntPathRequestMatcher("/api/v1/auth/logout", "POST")))
                    // Stateless, every bearer request counts as a fresh authentication, and the
                    // default strategy "rotates" the token on each one - by deleting the cookie.
                    // The next logout then went out without it, got a 403, and the session
                    // survived. The token guards the refresh cookie, not a login, so it stays.
                    .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy());
                csrf.addObjectPostProcessor(csrfErrorsFollowTheErrorContract(accessDeniedHandler));
            })
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers(PUBLIC_AUTH_PATHS).permitAll()
                // Request counts by route and error code say how the platform is being used.
                .requestMatchers("/actuator/metrics", "/actuator/metrics/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(new CsrfCookieFilter(), JwtAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Gives the CSRF filter the same error handler as the rest of the chain.
     *
     * <p>Configuring {@code exceptionHandling().accessDeniedHandler(…)} is not enough: it wires
     * {@code ExceptionTranslationFilter}, which sits <em>after</em> {@link CsrfFilter} and so
     * never sees what that filter rejects. The CSRF 403 would keep falling through to the
     * container's error page while every other 403 followed the contract - the worst outcome,
     * because the inconsistency would look like a fixed bug.
     *
     * <p>{@code CsrfConfigurer} exposes no setter for it, hence the post processor; Spring
     * Security only applies it to objects of the declared type.
     */
    private static ObjectPostProcessor<CsrfFilter> csrfErrorsFollowTheErrorContract(
            ProblemDetailAccessDeniedHandler accessDeniedHandler) {
        return new ObjectPostProcessor<>() {
            @Override
            public <O extends CsrfFilter> O postProcess(O filter) {
                filter.setAccessDeniedHandler(accessDeniedHandler);
                return filter;
            }
        };
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
