package com.tennisplatform.identity.adapters.in.web;

import com.tennisplatform.identity.domain.AccountNotActiveException;
import com.tennisplatform.identity.domain.InvalidCredentialsException;
import com.tennisplatform.identity.domain.InvalidTokenException;
import com.tennisplatform.identity.domain.TokenReuseDetectedException;
import com.tennisplatform.identity.domain.WeakPasswordException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps identity failures to Problem Details.
 *
 * <p>Every message here is deliberately vague. The service layer knows exactly what went
 * wrong - unknown email, wrong password, disabled account, expired token, reused token - and
 * none of that reaches the client, because the difference between those answers is precisely
 * what an attacker needs to map accounts and tokens.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {AuthController.class, MeController.class})
class IdentityExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentication failed",
                "Email or password is incorrect.", "AUTH_INVALID_CREDENTIALS");
    }

    /**
     * Same 401 the client would get for any expired token: it must not learn that theft was
     * detected, nor that a family was revoked. The server side is what logs and acts on it.
     */
    @ExceptionHandler(TokenReuseDetectedException.class)
    ProblemDetail handleTokenReuse(TokenReuseDetectedException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Session expired",
                "Please sign in again.", "AUTH_SESSION_EXPIRED");
    }

    @ExceptionHandler(InvalidTokenException.class)
    ProblemDetail handleInvalidToken(InvalidTokenException e) {
        return problem(HttpStatus.CONFLICT, "Invalid token",
                "The link is invalid, has expired or has already been used.", "AUTH_INVALID_TOKEN");
    }

    @ExceptionHandler(AccountNotActiveException.class)
    ProblemDetail handleAccountNotActive(AccountNotActiveException e) {
        return problem(HttpStatus.FORBIDDEN, "Account not active",
                "This account cannot perform that action.", "AUTH_ACCOUNT_NOT_ACTIVE");
    }

    /** The only one that may be specific: the user needs to know what to fix. */
    @ExceptionHandler(WeakPasswordException.class)
    ProblemDetail handleWeakPassword(WeakPasswordException e) {
        return problem(HttpStatus.BAD_REQUEST, "Password does not meet the policy",
                e.getMessage(), "AUTH_WEAK_PASSWORD");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
