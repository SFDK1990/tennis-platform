package com.tennisplatform.identity.adapters.in.web;

import com.tennisplatform.error.Problems;
import com.tennisplatform.identity.domain.AccountNotActiveException;
import com.tennisplatform.identity.domain.InvalidCredentialsException;
import com.tennisplatform.identity.domain.InvalidTokenException;
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
 *
 * <p>It used to name its controllers, one of which was {@code MeController}. That endpoint is
 * now composed at the web edge out of three modules, so naming controllers here would mean
 * either reaching across a boundary to name one or letting identity's failures fall through to
 * a generic 500. Selecting by exception type keeps the mapping with the module that owns the
 * failure, wherever the call came in.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class IdentityExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException e) {
        return Problems.of(HttpStatus.UNAUTHORIZED, "Authentication failed",
                "Email or password is incorrect.", "AUTH_INVALID_CREDENTIALS");
    }

    @ExceptionHandler(InvalidTokenException.class)
    ProblemDetail handleInvalidToken(InvalidTokenException e) {
        return Problems.of(HttpStatus.CONFLICT, "Invalid token",
                "The link is invalid, has expired or has already been used.", "AUTH_INVALID_TOKEN");
    }

    @ExceptionHandler(AccountNotActiveException.class)
    ProblemDetail handleAccountNotActive(AccountNotActiveException e) {
        return Problems.of(HttpStatus.FORBIDDEN, "Account not active",
                "This account cannot perform that action.", "AUTH_ACCOUNT_NOT_ACTIVE");
    }

    /** The only one that may be specific: the user needs to know what to fix. */
    @ExceptionHandler(WeakPasswordException.class)
    ProblemDetail handleWeakPassword(WeakPasswordException e) {
        return Problems.of(HttpStatus.BAD_REQUEST, "Password does not meet the policy",
                e.getMessage(), "AUTH_WEAK_PASSWORD");
    }
}
