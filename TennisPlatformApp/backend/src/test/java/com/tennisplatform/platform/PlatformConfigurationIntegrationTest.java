package com.tennisplatform.platform;

import com.tennisplatform.AbstractIntegrationTest;
import com.tennisplatform.platform.application.port.in.GetStudentLimit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The read side of the platform configuration against a real PostgreSQL.
 *
 * <p>What it actually proves is that the {@code v4-platform} changeset did its job: the
 * singleton row exists from the first migration, so nothing has to invent a limit at runtime.
 * A service test with a mocked repository could not tell whether the row was ever seeded.
 */
class PlatformConfigurationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private GetStudentLimit studentLimit;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theMigrationSeedsTheConfigurationRow() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM platform_configuration",
                Integer.class)).isEqualTo(1);
        assertThat(studentLimit.studentLimit()).isEqualTo(50);
    }

    /**
     * The row is restored by the per-test cleanup, which matters because the cleanup does wipe
     * it: TRUNCATE CASCADE reaches it through the {@code updated_by} foreign key to
     * {@code users}. It asserts on the row count rather than on the limit, because the fallback
     * happens to be the same number and would make a missing row look like a passing test.
     */
    @Test
    void theConfigurationSurvivesTheCleanupBetweenTests() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM platform_configuration",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void theSchemaRefusesASecondConfigurationRow() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO platform_configuration (id, student_limit) VALUES (2, 10)"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void theSchemaRefusesALimitThatWouldBlockEverybody() {
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE platform_configuration SET student_limit = 0 WHERE id = 1"))
                .isInstanceOf(Exception.class);
    }
}
