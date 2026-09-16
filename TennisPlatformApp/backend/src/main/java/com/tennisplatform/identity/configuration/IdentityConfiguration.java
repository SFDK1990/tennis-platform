package com.tennisplatform.identity.configuration;

import com.tennisplatform.identity.application.port.in.GetCurrentUser;
import com.tennisplatform.identity.application.port.in.Login;
import com.tennisplatform.identity.application.port.in.Logout;
import com.tennisplatform.identity.application.port.in.RefreshSession;
import com.tennisplatform.identity.application.port.in.RegisterUser;
import com.tennisplatform.identity.application.port.in.RequestPasswordReset;
import com.tennisplatform.identity.application.port.in.ResetPassword;
import com.tennisplatform.identity.application.port.in.VerifyEmail;
import com.tennisplatform.identity.application.port.out.AccessTokenIssuer;
import com.tennisplatform.identity.application.port.out.EmailVerificationTokens;
import com.tennisplatform.identity.application.port.out.IdentityMailer;
import com.tennisplatform.identity.application.port.out.PasswordHasher;
import com.tennisplatform.identity.application.port.out.PasswordResetTokens;
import com.tennisplatform.identity.application.port.out.RefreshTokens;
import com.tennisplatform.identity.application.port.out.SecureTokenGenerator;
import com.tennisplatform.identity.application.port.out.TokenHasher;
import com.tennisplatform.identity.application.port.out.UserRepository;
import com.tennisplatform.identity.application.service.GetCurrentUserService;
import com.tennisplatform.identity.application.service.LoginService;
import com.tennisplatform.identity.application.service.LogoutService;
import com.tennisplatform.identity.application.service.RefreshSessionService;
import com.tennisplatform.identity.application.service.RegisterUserService;
import com.tennisplatform.identity.application.service.RequestPasswordResetService;
import com.tennisplatform.identity.application.service.ResetPasswordService;
import com.tennisplatform.identity.application.service.VerifyEmailService;
import com.tennisplatform.identity.adapters.in.web.AuthRateLimitFilter;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.Map;

/**
 * Wires the use cases by hand instead of component-scanning them. They need configuration
 * values (the token lifetimes) that are not beans, and keeping the annotations out of the
 * application layer leaves it almost free of framework coupling.
 */
@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityConfiguration {

    /**
     * Delegating encoder: every hash is stored with its algorithm as a prefix
     * ({@code {bcrypt}$2a$12$...}). Moving to Argon2id later then means adding it to this map
     * and changing the default id - existing passwords keep verifying and are re-encoded as
     * users log in, instead of everyone being locked out.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("bcrypt",
                Map.of("bcrypt", new BCryptPasswordEncoder(12)));
    }

    @Bean
    public RegisterUser registerUser(UserRepository users, EmailVerificationTokens verificationTokens,
                                     PasswordHasher passwordHasher, SecureTokenGenerator tokenGenerator,
                                     TokenHasher tokenHasher, IdentityMailer mailer, Clock clock,
                                     IdentityProperties properties) {
        return new RegisterUserService(users, verificationTokens, passwordHasher, tokenGenerator,
                tokenHasher, mailer, clock, properties.getEmailVerificationTtl());
    }

    @Bean
    public VerifyEmail verifyEmail(EmailVerificationTokens verificationTokens, UserRepository users,
                                   TokenHasher tokenHasher, Clock clock) {
        return new VerifyEmailService(verificationTokens, users, tokenHasher, clock);
    }

    @Bean
    public GetCurrentUser getCurrentUser(UserRepository users) {
        return new GetCurrentUserService(users);
    }

    /**
     * Registered explicitly rather than component-scanned so its order is deliberate: the
     * limiter runs ahead of the security chain, and a flood of requests is rejected before
     * any authentication work - password hashing above all - is performed.
     */
    @Bean
    public FilterRegistrationBean<AuthRateLimitFilter> authRateLimitFilter(IdentityProperties properties) {
        FilterRegistrationBean<AuthRateLimitFilter> registration =
                new FilterRegistrationBean<>(new AuthRateLimitFilter(properties));
        registration.addUrlPatterns("/api/v1/auth/*");
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 10);
        return registration;
    }

    @Bean
    public Login login(UserRepository users, RefreshTokens refreshTokens, PasswordHasher passwordHasher,
                       SecureTokenGenerator tokenGenerator, TokenHasher tokenHasher,
                       AccessTokenIssuer accessTokenIssuer, Clock clock, IdentityProperties properties) {
        return new LoginService(users, refreshTokens, passwordHasher, tokenGenerator, tokenHasher,
                accessTokenIssuer, clock, properties.getRefreshTokenTtl());
    }

    @Bean
    public RefreshSession refreshSession(RefreshTokens refreshTokens, UserRepository users,
                                         SecureTokenGenerator tokenGenerator, TokenHasher tokenHasher,
                                         AccessTokenIssuer accessTokenIssuer, Clock clock,
                                         IdentityProperties properties) {
        return new RefreshSessionService(refreshTokens, users, tokenGenerator, tokenHasher,
                accessTokenIssuer, clock, properties.getRefreshTokenTtl());
    }

    @Bean
    public Logout logout(RefreshTokens refreshTokens, TokenHasher tokenHasher, Clock clock) {
        return new LogoutService(refreshTokens, tokenHasher, clock);
    }

    @Bean
    public RequestPasswordReset requestPasswordReset(UserRepository users, PasswordResetTokens resetTokens,
                                                     SecureTokenGenerator tokenGenerator,
                                                     TokenHasher tokenHasher, IdentityMailer mailer,
                                                     Clock clock, IdentityProperties properties) {
        return new RequestPasswordResetService(users, resetTokens, tokenGenerator, tokenHasher,
                mailer, clock, properties.getPasswordResetTtl());
    }

    @Bean
    public ResetPassword resetPassword(PasswordResetTokens resetTokens, UserRepository users,
                                       RefreshTokens refreshTokens, PasswordHasher passwordHasher,
                                       TokenHasher tokenHasher, Clock clock) {
        return new ResetPasswordService(resetTokens, users, refreshTokens, passwordHasher,
                tokenHasher, clock);
    }
}
