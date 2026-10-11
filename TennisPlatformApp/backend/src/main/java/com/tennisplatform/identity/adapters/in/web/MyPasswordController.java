package com.tennisplatform.identity.adapters.in.web;

import com.tennisplatform.identity.adapters.in.web.AuthDtos.ChangePasswordRequest;
import com.tennisplatform.identity.adapters.in.web.AuthDtos.LoginResponse;
import com.tennisplatform.identity.adapters.in.web.AuthDtos.UserSummaryResponse;
import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.identity.application.port.in.AuthenticationResult;
import com.tennisplatform.identity.application.port.in.ChangePassword;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Duration;

/**
 * {@code POST /me/password}. Under {@code /me} because it is the caller's own account, but here
 * and not in {@code web}'s {@code MeController}: it touches nothing but identity.
 *
 * <p>It answers like the login - a fresh access token in the body and a fresh refresh cookie -
 * because the session this browser had was revoked along with the others.
 */
@RestController
class MyPasswordController {

    private final ChangePassword changePassword;
    private final RefreshTokenCookie refreshCookie;
    private final Clock clock;

    MyPasswordController(ChangePassword changePassword, RefreshTokenCookie refreshCookie, Clock clock) {
        this.changePassword = changePassword;
        this.refreshCookie = refreshCookie;
        this.clock = clock;
    }

    @PostMapping(AuthRateLimitFilter.CHANGE_PASSWORD)
    ResponseEntity<LoginResponse> change(@AuthenticationPrincipal AuthenticatedUser caller,
                                         @Valid @RequestBody ChangePasswordRequest request) {
        AuthenticationResult result = changePassword.change(
                new ChangePassword.Command(caller.id(), request.currentPassword(), request.newPassword()));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.issue(result.refreshToken(),
                        Duration.between(clock.instant(), result.refreshTokenExpiresAt())))
                .body(new LoginResponse(result.accessToken(), result.accessTokenExpiresAt(),
                        UserSummaryResponse.from(result.user())));
    }
}
