package com.tennisplatform.identity.configuration;

import com.tennisplatform.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The only way an administrator comes to exist (23-fase12-analisis-administracion.md), so it has
 * to work on the first startup and change nothing on the next ones. Untested until the coverage
 * of Fase 14 showed it.
 *
 * <p>Same shape as {@code TeacherBootstrapIdempotencyTest}: the credentials are set here so the
 * rest of the suite runs with no admin at all.
 */
@TestPropertySource(properties = {
        "tennis.identity.admin-bootstrap-email=bootstrap-admin@tennis-platform.local",
        "tennis.identity.admin-bootstrap-password=a-valid-password"
})
class AdminBootstrapTest extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "bootstrap-admin@tennis-platform.local";

    @Autowired
    private AdminBootstrap bootstrap;

    @Autowired
    private JdbcTemplate jdbc;

    /** The base class empties the database, which removes what the startup run created. */
    @BeforeEach
    void runTheBootstrapOnce() {
        bootstrap.run(null);
    }

    @Test
    void theAdministratorItCreatesCanSignIn() {
        var login = rest.postForEntity("/api/v1/auth/login",
                Map.of("email", ADMIN_EMAIL, "password", "a-valid-password"), Map.class);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) login.getBody().get("user")).get("role")).isEqualTo("ADMIN");
    }

    @Test
    void runningItAgainLeavesTheExistingAccountAlone() {
        String hash = passwordHash();

        bootstrap.run(null);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE role = 'ADMIN'", Integer.class))
                .isEqualTo(1);
        assertThat(passwordHash()).isEqualTo(hash);
    }

    private String passwordHash() {
        return jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, ADMIN_EMAIL);
    }
}
