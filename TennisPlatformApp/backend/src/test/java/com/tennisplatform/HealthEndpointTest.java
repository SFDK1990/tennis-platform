package com.tennisplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HealthEndpointTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "health-password";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void healthEndpointIsPublicAndReportsUp() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void businessApiIsClosedByDefault() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/anything", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** The components name the database and the disk; that is for whoever runs the platform. */
    @Test
    void onlyAnAdminSeesWhatTheHealthIsMadeOf() {
        String student = tokenOfANewStudent(PASSWORD);
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.com";
        rest.postForEntity("/api/v1/auth/register", Map.of("email", adminEmail, "password", PASSWORD), String.class);
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", adminEmail);
        String admin = tokenOf(adminEmail, PASSWORD);

        assertThat(health(student)).doesNotContain("components");
        assertThat(health(admin)).contains("\"db\"");
    }

    private String health(String token) {
        return rest.exchange("/actuator/health", HttpMethod.GET, new HttpEntity<>(bearer(token)), String.class)
                .getBody();
    }
}
