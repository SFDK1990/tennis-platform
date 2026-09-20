package com.tennisplatform.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two branches of the handler, without a server.
 *
 * <p>The plain authorization branch has no endpoint that reaches it today - every business 403
 * is thrown by the modules and mapped by their own advices - so this is the only place it is
 * exercised. A default that nothing tests is a default that quietly rots.
 */
class ProblemDetailAccessDeniedHandlerTest {

    // Built the way Spring Boot builds the application's own mapper, which is what registers
    // the ProblemDetail mixin: a plain ObjectMapper nests the extra fields under "properties"
    // and the assertions below would pass while the real payload was the wrong shape.
    private final ProblemDetailAccessDeniedHandler handler = new ProblemDetailAccessDeniedHandler(
            new ProblemDetailWriter(Jackson2ObjectMapperBuilder.json().build()));

    @Test
    void aCsrfRejectionNamesTheHeaderTheCallerIsMissing() throws IOException {
        MockHttpServletResponse response = handle(new MissingCsrfTokenException("token"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"code\":\"AUTH_CSRF_TOKEN_INVALID\"")
                .contains("X-XSRF-TOKEN");
    }

    @Test
    void anyOtherDenialIsReportedAsAPlainAuthorizationFailure() throws IOException {
        MockHttpServletResponse response = handle(new AccessDeniedException("nope"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .contains("\"code\":\"AUTH_FORBIDDEN\"")
                .doesNotContain("X-XSRF-TOKEN");
    }

    /** A response already on its way out must not be rewritten. */
    @Test
    void aCommittedResponseIsLeftAlone() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        response.flushBuffer();

        handler.handle(request, response, new AccessDeniedException("nope"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEmpty();
    }

    private MockHttpServletResponse handle(AccessDeniedException exception) throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.handle(request, response, exception);
        return response;
    }
}
