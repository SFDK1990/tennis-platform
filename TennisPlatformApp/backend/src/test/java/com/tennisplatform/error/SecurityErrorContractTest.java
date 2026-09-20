package com.tennisplatform.error;

import com.tennisplatform.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rejections produced by the security filters, checked against 11-contrato-api.md.
 *
 * <p>These two paths never reach Spring MVC, so no controller advice covers them and the
 * ordinary API tests cannot see them: before this was fixed a 401 came back with an empty body
 * and a CSRF 403 with the container's {@code {"timestamp":…}} page, while every documented
 * response said {@code application/problem+json}.
 */
class SecurityErrorContractTest extends AbstractIntegrationTest {

    @Test
    @SuppressWarnings("rawtypes")
    void anUnauthenticatedRequestAnswersWithTheErrorContract() {
        ResponseEntity<Map> response = rest.getForEntity("/api/v1/me", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType())
                .isNotNull()
                .matches(type -> type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(response.getBody())
                .containsEntry("code", "AUTH_UNAUTHENTICATED")
                .containsEntry("status", 401)
                .containsEntry("instance", "/api/v1/me")
                .containsKeys("title", "detail");
    }

    /**
     * The reason the finding was worth fixing: the frontend has to be told that the missing
     * piece is a header, not a permission.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void refreshingWithoutTheCsrfHeaderSaysSoInTheErrorContract() {
        ResponseEntity<Map> response = rest.exchange("/api/v1/auth/refresh", HttpMethod.POST,
                new HttpEntity<>(null, new HttpHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getHeaders().getContentType())
                .isNotNull()
                .matches(type -> type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(response.getBody())
                .containsEntry("code", "AUTH_CSRF_TOKEN_INVALID")
                .containsEntry("status", 403);
        assertThat((String) response.getBody().get("detail")).contains("X-XSRF-TOKEN");
    }

    /** Logout is protected by the same matcher and must answer the same way. */
    @Test
    @SuppressWarnings("rawtypes")
    void loggingOutWithoutTheCsrfHeaderSaysSoInTheErrorContract() {
        ResponseEntity<Map> response = rest.exchange("/api/v1/auth/logout", HttpMethod.POST,
                new HttpEntity<>(null, new HttpHeaders()), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("code", "AUTH_CSRF_TOKEN_INVALID");
    }
}
