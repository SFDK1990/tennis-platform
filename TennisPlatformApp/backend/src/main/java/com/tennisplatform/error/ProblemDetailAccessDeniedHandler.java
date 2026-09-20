package com.tennisplatform.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Answers a denied request with the error contract instead of the container's error page.
 *
 * <p>The default handler calls {@code response.sendError}, which hands the request back to the
 * servlet container, which forwards it to {@code /error}, which answers with Spring Boot's
 * {@code {"timestamp","status","error","path"}}. That is a different shape from every other
 * error the API produces, and a client parsing {@code code} finds nothing there.
 *
 * <p>A CSRF rejection gets its own code because it is the one 403 the caller can actually fix:
 * {@code /auth/refresh} and {@code /auth/logout} authenticate by cookie alone and therefore
 * require the double-submit header. Telling a frontend "forbidden" when the real answer is
 * "echo back the XSRF-TOKEN cookie in X-XSRF-TOKEN" costs an afternoon; this already happened
 * once during the manual run of 19/09/2026.
 */
@Component
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailWriter writer;

    ProblemDetailAccessDeniedHandler(ProblemDetailWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        if (exception instanceof CsrfException) {
            writer.write(request, response, HttpStatus.FORBIDDEN, "AUTH_CSRF_TOKEN_INVALID",
                    "This endpoint is authenticated by cookie and requires the CSRF token: send "
                            + "the value of the XSRF-TOKEN cookie in the X-XSRF-TOKEN header");
            return;
        }
        writer.write(request, response, HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                "The authenticated caller is not allowed to perform this operation");
    }
}
