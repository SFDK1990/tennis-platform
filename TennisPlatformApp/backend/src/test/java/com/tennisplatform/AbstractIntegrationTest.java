package com.tennisplatform;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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

    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }
}
