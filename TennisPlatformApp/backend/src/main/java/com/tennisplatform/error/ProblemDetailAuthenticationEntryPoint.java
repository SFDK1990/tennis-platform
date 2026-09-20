package com.tennisplatform.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Answers an unauthenticated request with the error contract instead of an empty body.
 *
 * <p>Replaces {@code HttpStatusEntryPoint}, which writes the status and nothing else: a client
 * receiving a bare 401 cannot tell an expired token from a missing one, and the frontend has no
 * {@code code} to branch on.
 *
 * <p>The detail is deliberately vague. Whether the token was absent, malformed, expired or
 * signed by someone else is information the caller has not earned, and saying so would let an
 * attacker probe which of their guesses is closer.
 */
@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailWriter writer;

    ProblemDetailAuthenticationEntryPoint(ProblemDetailWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        writer.write(request, response, HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHENTICATED",
                "Authentication is required and the credentials presented are missing or invalid");
    }
}
