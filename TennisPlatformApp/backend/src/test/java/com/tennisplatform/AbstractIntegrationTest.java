package com.tennisplatform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for tests that need a real PostgreSQL.
 *
 * <p>Singleton container pattern on purpose: the container is started once for the whole
 * test run and never stopped. Using {@code @Container} instead would stop it at the end of
 * the first test class, leaving every later class with a dead database. Cleanup is Ryuk's
 * job once the JVM exits.
 *
 * <p>{@link EnabledIf} makes subclasses skip - rather than fail - on machines with no
 * Docker daemon.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIf(value = "com.tennisplatform.AbstractIntegrationTest#dockerAvailable",
           disabledReason = "No Docker daemon available")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        if (dockerAvailable()) {
            POSTGRES.start();
        }
    }

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * Every integration test starts from an empty database.
     *
     * <p>One container is shared by the whole run, so without this each class inherits whatever
     * the previous one left behind and the result depends on the execution order - which
     * surefire does not keep identical across platforms. That is not hypothetical: it broke the
     * pipeline on its first run in Fase 5.1, with a build that was green on Windows.
     *
     * <p>TRUNCATE rather than DELETE because it ignores insertion order, and CASCADE so foreign
     * keys do not force the list to be kept in dependency order. New tables must be added here
     * as their modules arrive - the alternative, each class cleaning up after itself, is exactly
     * the arrangement that already failed once.
     *
     * <p>{@code platform_configuration} is not in the list and is emptied anyway: CASCADE
     * follows its {@code updated_by} foreign key to {@code users}. Leaving it out is therefore
     * not enough - the row has to be put back, or every test after the first would silently run
     * against the fallback limit instead of the configured one, which would surface as a
     * puzzling number in some unrelated assertion rather than as a failure here. It is seeded
     * with the same value as the {@code v4-platform} changeset.
     */
    @BeforeEach
    void emptyTheDatabase() {
        jdbc.execute("TRUNCATE TABLE teacher_students, student_profiles, teacher_profiles, "
                + "refresh_tokens, password_reset_tokens, email_verifications, users CASCADE");
        jdbc.update("INSERT INTO platform_configuration (id, student_limit) VALUES (1, 50) "
                + "ON CONFLICT (id) DO NOTHING");
    }

    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }
}
