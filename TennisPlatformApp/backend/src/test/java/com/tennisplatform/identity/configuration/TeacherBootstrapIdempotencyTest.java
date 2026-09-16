package com.tennisplatform.identity.configuration;

import com.tennisplatform.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Closes the open acceptance criterion of Fase 5: the bootstrap is documented as idempotent,
 * but nothing demonstrated it.
 *
 * <p>It matters because the runner executes on <em>every</em> startup, and a container that
 * restarts is the normal case, not the exception. The test lives in this package because
 * {@link TeacherBootstrap} is package-private on purpose - it is wiring, not public API.
 *
 * <p>The credentials are supplied here rather than in {@code application-test.yml} so the rest
 * of the suite keeps running with the bootstrap disabled.
 */
@TestPropertySource(properties = {
        "tennis.identity.bootstrap-teacher-email=bootstrap-teacher@tennis-platform.local",
        "tennis.identity.bootstrap-teacher-password=a-valid-password"
})
class TeacherBootstrapIdempotencyTest extends AbstractIntegrationTest {

    private static final String TEACHER_EMAIL = "bootstrap-teacher@tennis-platform.local";

    @Autowired
    private TeacherBootstrap bootstrap;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * The integration tests share one database (a singleton container), so a class that leaves
     * a teacher behind breaks whichever class runs next - and surefire does not order classes
     * the same way on Windows and on Linux. Cleaning both before and after keeps this class
     * independent of the order and of what the others left.
     */
    @BeforeEach
    @AfterEach
    void removeAnyTeacher() {
        jdbc.update("DELETE FROM users WHERE role = 'TEACHER'");
    }

    @Test
    void runningTheBootstrapAgainNeverCreatesASecondTeacher() {
        bootstrap.run(null);
        assertThat(teacherCount()).isEqualTo(1);

        bootstrap.run(null);
        bootstrap.run(null);

        assertThat(teacherCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT email FROM users WHERE role = 'TEACHER'", String.class))
                .isEqualTo(TEACHER_EMAIL);
    }

    private Integer teacherCount() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE role = 'TEACHER'", Integer.class);
    }
}
