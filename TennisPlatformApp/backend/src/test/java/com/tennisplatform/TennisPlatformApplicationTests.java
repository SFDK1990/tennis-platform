package com.tennisplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class TennisPlatformApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        assertThat(jdbcTemplate).isNotNull();
    }

    @Test
    void liquibaseRanTheScaffoldChangeSetFromAnEmptyDatabase() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM databasechangelog WHERE id = 'v0-scaffold-extensions'",
                Integer.class);

        assertThat(applied).isEqualTo(1);
    }

    @Test
    void requiredPostgresExtensionsAreInstalled() {
        Integer extensions = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname IN ('pgcrypto', 'btree_gist')",
                Integer.class);

        assertThat(extensions).isEqualTo(2);
    }
}
