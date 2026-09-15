package com.tennisplatform.identity.adapters.in.web;

import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.identity.domain.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire contract, kept separate from the domain so the API can stay stable while the domain
 * evolves. Shapes follow openapi.yaml.
 *
 * <p>Note what is absent: no role field anywhere. Registration always produces a STUDENT, and
 * accepting a role from the client would be privilege escalation waiting to happen.
 */
final class AuthDtos {

    private AuthDtos() {
    }

    record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = 200) String password) {
    }

    record LoginRequest(
            @NotBlank String email,
            @NotBlank String password) {
    }

    record VerifyEmailRequest(@NotBlank String token) {
    }

    record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = 200) String newPassword) {
    }

    record UserSummaryResponse(UUID id, String email, String role, String status) {

        static UserSummaryResponse from(UserSummary summary) {
            return new UserSummaryResponse(summary.id(), summary.email(),
                    summary.role().name(), summary.status().name());
        }
    }

    /**
     * The refresh token is deliberately absent: it only ever travels in a Set-Cookie header,
     * never in a body where a script could read it.
     */
    record LoginResponse(String accessToken, Instant accessTokenExpiresAt, UserSummaryResponse user) {
    }

    record AccessTokenResponse(String accessToken, Instant accessTokenExpiresAt) {
    }
}
