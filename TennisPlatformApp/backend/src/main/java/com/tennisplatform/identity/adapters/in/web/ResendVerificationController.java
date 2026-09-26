package com.tennisplatform.identity.adapters.in.web;

import com.tennisplatform.identity.application.port.in.AuthenticatedUser;
import com.tennisplatform.identity.application.port.in.ResendEmailVerification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asked for by the signed-in user, never by address: taking an email here would let anyone
 * flood someone else's inbox. It lives under /auth so the per-IP rate limit covers it.
 */
@RestController
class ResendVerificationController {

    private final ResendEmailVerification resendEmailVerification;

    ResendVerificationController(ResendEmailVerification resendEmailVerification) {
        this.resendEmailVerification = resendEmailVerification;
    }

    /** 202 whether or not a link was sent: an already verified address simply needs none. */
    @PostMapping("/api/v1/auth/verification-email")
    ResponseEntity<Void> resend(@AuthenticationPrincipal AuthenticatedUser caller) {
        resendEmailVerification.resend(caller.id());
        return ResponseEntity.accepted().build();
    }
}
