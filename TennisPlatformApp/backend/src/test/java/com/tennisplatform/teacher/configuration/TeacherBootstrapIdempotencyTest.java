package com.tennisplatform.teacher.configuration;

import com.tennisplatform.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The bootstrap runs on every single startup, and a container that restarts is the normal case,
 * so running it again must never produce a second teacher - nor a second profile.
 *
 * <p>Fase 5 left this criterion open for the account. Fase 6 extends it to the profile, which is
 * the half that did not exist before.
 *
 * <p>Lives in this package because {@link TeacherBootstrap} is package-private on purpose: it is
 * wiring, not public API. The credentials are supplied here rather than in
 * {@code application-test.yml} so the rest of the suite keeps running with no teacher at all.
 */
@TestPropertySource(properties = {
        "tennis.teacher.bootstrap-email=bootstrap-teacher@tennis-platform.local",
        "tennis.teacher.bootstrap-password=a-valid-password",
        "tennis.teacher.bootstrap-display-name=Ana Serrano",
        "tennis.teacher.bootstrap-timezone=Europe/Madrid"
})
class TeacherBootstrapIdempotencyTest extends AbstractIntegrationTest {

    private static final String TEACHER_EMAIL = "bootstrap-teacher@tennis-platform.local";

    @Autowired
    private TeacherBootstrap bootstrap;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * The base class empties the database before each test, which also removes what the runner
     * created when the context started. Seeding again here is what keeps this class independent
     * of that - and it is the first of the three executions the test is about.
     */
    @BeforeEach
    void runTheBootstrapOnce() {
        bootstrap.run(null);
    }

    @Test
    void runningTheBootstrapAgainNeverCreatesASecondTeacher() {
        assertThat(teacherCount()).isEqualTo(1);
        assertThat(profileCount()).isEqualTo(1);

        bootstrap.run(null);
        bootstrap.run(null);

        assertThat(teacherCount()).isEqualTo(1);
        assertThat(profileCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT email FROM users WHERE role = 'TEACHER'", String.class))
                .isEqualTo(TEACHER_EMAIL);
    }

    /** The profile the ER diagram requires, which Fase 5 never created. */
    @Test
    void createsTheProfileAlongsideTheAccount() {
        assertThat(jdbc.queryForObject(
                "SELECT display_name FROM teacher_profiles", String.class))
                .isEqualTo("Ana Serrano");
        assertThat(jdbc.queryForObject("SELECT timezone FROM teacher_profiles", String.class))
                .isEqualTo("Europe/Madrid");

        // The profile belongs to the account, not to a row of its own.
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM teacher_profiles p JOIN users u ON u.id = p.user_id "
                        + "WHERE u.role = 'TEACHER'", Integer.class))
                .isEqualTo(1);
    }

    /**
     * An existing teacher is left untouched, so changing the environment variable later cannot
     * silently take over an account that is already in use.
     */
    @Test
    void doesNotOverwriteAnExistingTeacherWithNewBootstrapValues() {
        jdbc.update("UPDATE teacher_profiles SET display_name = 'Renamed by the teacher'");

        bootstrap.run(null);

        assertThat(jdbc.queryForObject(
                "SELECT display_name FROM teacher_profiles", String.class))
                .isEqualTo("Renamed by the teacher");
    }

    private Integer teacherCount() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE role = 'TEACHER'", Integer.class);
    }

    private Integer profileCount() {
        return jdbc.queryForObject("SELECT count(*) FROM teacher_profiles", Integer.class);
    }
}
