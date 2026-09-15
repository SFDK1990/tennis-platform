package com.tennisplatform.identity.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "tennis.identity")
public class IdentityProperties {

    /**
     * HS256 requires at least 256 bits of key material, so the minimum length is enforced
     * here rather than failing deep inside the JWT library at the first login.
     */
    @NotBlank
    @Size(min = 32, message = "tennis.identity.jwt-secret must be at least 32 characters")
    private String jwtSecret;

    @NotBlank
    private String jwtIssuer = "tennis-platform";

    /** Short by design: it bounds how long a stolen access token remains usable. */
    @NotNull
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    @NotNull
    private Duration refreshTokenTtl = Duration.ofDays(14);

    @NotNull
    private Duration emailVerificationTtl = Duration.ofDays(1);

    /** Deliberately shorter than verification: a reset link is a live key to the account. */
    @NotNull
    private Duration passwordResetTtl = Duration.ofHours(1);

    /** Used to build the links sent by email; the pages live in the frontend, not the API. */
    @NotBlank
    private String frontendBaseUrl = "http://localhost:3000";

    @NotBlank
    private String mailFrom = "no-reply@tennis-platform.local";

    /**
     * Secure flag of the refresh cookie. Defaults to true and is only turned off for local
     * development, where there is no HTTPS - a browser silently drops a Secure cookie on
     * plain http, which would look like "login does not work".
     */
    private boolean cookieSecure = true;

    /**
     * Teacher bootstrap credentials, read from the environment. Deliberately not seeded from a
     * Liquibase changeset: changelogs are versioned files in a published repository, and a
     * password - or its hash - would stay in the Git history forever.
     */
    private String bootstrapTeacherEmail;

    private String bootstrapTeacherPassword;

    /** Requests per minute per client IP allowed on the authentication endpoints. */
    private int authRateLimitPerMinute = 20;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }

    public void setJwtIssuer(String jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public Duration getEmailVerificationTtl() {
        return emailVerificationTtl;
    }

    public void setEmailVerificationTtl(Duration emailVerificationTtl) {
        this.emailVerificationTtl = emailVerificationTtl;
    }

    public Duration getPasswordResetTtl() {
        return passwordResetTtl;
    }

    public void setPasswordResetTtl(Duration passwordResetTtl) {
        this.passwordResetTtl = passwordResetTtl;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getBootstrapTeacherEmail() {
        return bootstrapTeacherEmail;
    }

    public void setBootstrapTeacherEmail(String bootstrapTeacherEmail) {
        this.bootstrapTeacherEmail = bootstrapTeacherEmail;
    }

    public String getBootstrapTeacherPassword() {
        return bootstrapTeacherPassword;
    }

    public void setBootstrapTeacherPassword(String bootstrapTeacherPassword) {
        this.bootstrapTeacherPassword = bootstrapTeacherPassword;
    }

    public int getAuthRateLimitPerMinute() {
        return authRateLimitPerMinute;
    }

    public void setAuthRateLimitPerMinute(int authRateLimitPerMinute) {
        this.authRateLimitPerMinute = authRateLimitPerMinute;
    }
}
