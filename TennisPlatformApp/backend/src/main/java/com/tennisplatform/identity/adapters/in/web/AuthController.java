package com.tennisplatform.identity.adapters.in.web;

import com.tennisplatform.identity.adapters.in.web.AuthDtos.AccessTokenResponse;
import com.tennisplatform.identity.adapters.in.web.AuthDtos.ForgotPasswordRequest;
import com.tennisplatform.identity.adapters.in.web.AuthDtos.LoginRequest;
import com.tennisplatform.identity.adapters.in.web.AuthDtos.LoginResponse;
import com.tennisplatform.identity.adapters.in.web.AuthDtos.RegisterRequest;
import com.tennisplatform.identity.adapters.in.web.AuthDtos.ResetPasswordRequest;
import com.tennisplatform.identity.adapters.in.web.AuthDtos.UserSummaryResponse;
import com.tennisplatform.identity.adapters.in.web.AuthDtos.VerifyEmailRequest;
import com.tennisplatform.identity.application.port.in.AuthenticationResult;
import com.tennisplatform.identity.application.port.in.Login;
import com.tennisplatform.identity.application.port.in.Logout;
import com.tennisplatform.identity.application.port.in.RefreshSession;
import com.tennisplatform.identity.application.port.in.RegisterUser;
import com.tennisplatform.identity.application.port.in.RequestPasswordReset;
import com.tennisplatform.identity.application.port.in.ResetPassword;
import com.tennisplatform.identity.application.port.in.VerifyEmail;
import com.tennisplatform.identity.domain.InvalidTokenException;
import com.tennisplatform.identity.domain.TokenReuseDetectedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final RegisterUser registerUser;
    private final VerifyEmail verifyEmail;
    private final Login login;
    private final RefreshSession refreshSession;
    private final Logout logout;
    private final RequestPasswordReset requestPasswordReset;
    private final ResetPassword resetPassword;
    private final RefreshTokenCookie refreshCookie;
    private final Clock clock;

    AuthController(RegisterUser registerUser, VerifyEmail verifyEmail, Login login,
                   RefreshSession refreshSession, Logout logout,
                   RequestPasswordReset requestPasswordReset, ResetPassword resetPassword,
                   RefreshTokenCookie refreshCookie, Clock clock) {
        this.registerUser = registerUser;
        this.verifyEmail = verifyEmail;
        this.login = login;
        this.refreshSession = refreshSession;
        this.logout = logout;
        this.requestPasswordReset = requestPasswordReset;
        this.resetPassword = resetPassword;
        this.refreshCookie = refreshCookie;
        this.clock = clock;
    }

    /**
     * Always 202, whether or not the address was already registered. Returning 201 for a new
     * account and 409 for an existing one - as openapi.yaml still describes - would let anyone
     * discover which emails have an account, which 08-security-engineer.md forbids.
     */
    @PostMapping("/register")
    ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        registerUser.register(new RegisterUser.Command(request.email(), request.password()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationResult result = login.login(new Login.Command(request.email(), request.password()));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFor(result))
                .body(new LoginResponse(result.accessToken(), result.accessTokenExpiresAt(),
                        UserSummaryResponse.from(result.user())));
    }

    /**
     * Failures here are 401, not the 409 that token failures get elsewhere: for the client
     * this is "your session ended, sign in again". Missing cookie, expired, revoked and reused
     * all look identical from outside - only the server knows which it was.
     *
     * <p>"Identical" has to include the body. The two failure paths used to answer differently -
     * one a Problem Detail with no {@code code}, the other an empty 401 - which both broke the
     * error contract and told a caller which of the two had happened.
     */
    @PostMapping("/refresh")
    ResponseEntity<Object> refresh(HttpServletRequest httpRequest) {
        String presented = refreshCookie.readFrom(httpRequest).orElse(null);
        if (presented == null) {
            return sessionExpired();
        }

        AuthenticationResult result;
        try {
            result = refreshSession.refresh(new RefreshSession.Command(presented));
        } catch (InvalidTokenException | TokenReuseDetectedException e) {
            return sessionExpired();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFor(result))
                .body(new AccessTokenResponse(result.accessToken(), result.accessTokenExpiresAt()));
    }

    /** Clears the dead cookie so the browser stops presenting it, and says why in the contract. */
    private ResponseEntity<Object> sessionExpired() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Session expired");
        problem.setDetail("Please sign in again.");
        problem.setProperty("code", "AUTH_SESSION_EXPIRED");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    /** Clears the cookie even when the token was unknown, so the browser never keeps a dead one. */
    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        refreshCookie.readFrom(httpRequest)
                .ifPresent(token -> logout.logout(new Logout.Command(token)));

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear())
                .build();
    }

    @PostMapping("/verify-email")
    ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmail.verify(new VerifyEmail.Command(request.token()));
        return ResponseEntity.noContent().build();
    }

    /** Always 202: replying differently for unknown addresses would leak which ones exist. */
    @PostMapping("/forgot-password")
    ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        requestPasswordReset.request(new RequestPasswordReset.Command(request.email()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPassword.reset(new ResetPassword.Command(request.token(), request.newPassword()));

        // Every session was just revoked, so the cookie in this browser is dead too.
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear())
                .build();
    }

    private String cookieFor(AuthenticationResult result) {
        return refreshCookie.issue(result.refreshToken(),
                Duration.between(clock.instant(), result.refreshTokenExpiresAt()));
    }
}
