package com.tennisplatform.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * Writes a {@link ProblemDetail} straight into the servlet response.
 *
 * <p>Exists because the security filters run outside Spring MVC: a rejection there never
 * reaches {@link GlobalExceptionHandler} or any message converter. Left to itself, Spring
 * answers those with an empty body (401) or with the container's default error page (403,
 * {@code {"timestamp":…}}), neither of which is the {@code application/problem+json} that
 * 11-contrato-api.md promises for every error.
 *
 * <p>The same {@link ObjectMapper} the rest of the API uses is injected on purpose, so a
 * problem written here is byte-for-byte the shape of one written by a controller advice.
 */
@Component
public class ProblemDetailWriter {

    private final ObjectMapper objectMapper;

    ProblemDetailWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String code, String detail) throws IOException {
        // A response already sent cannot be rewritten; overwriting the status here would only
        // throw on top of whatever error is already on its way out.
        if (response.isCommitted()) {
            return;
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), problem);
        response.flushBuffer();
    }
}
