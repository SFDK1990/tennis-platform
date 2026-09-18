package com.tennisplatform.teacher.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bootstrap data for the single teacher, read from the environment.
 *
 * <p>Deliberately not seeded from a Liquibase changeset: changelogs are versioned files in a
 * repository that is already published, and a password - or its hash - would stay in the Git
 * history forever.
 *
 * <p>These properties moved here from {@code tennis.identity} in Fase 6, when the bootstrap
 * became the teacher module's job: it now creates the account <em>and</em> the profile, and the
 * profile is not something identity is allowed to know about.
 */
@ConfigurationProperties(prefix = "tennis.teacher")
public class TeacherProperties {

    /** No teacher is created when this is blank, which is the default in development. */
    private String bootstrapEmail;

    private String bootstrapPassword;

    private String bootstrapDisplayName;

    /**
     * IANA time zone of the teacher, the reference for every lesson shown in local time. It has
     * a default because a missing zone would leave the profile unusable, while a wrong one is
     * visible immediately and can be corrected from the API.
     */
    private String bootstrapTimezone = "Europe/Madrid";

    public String getBootstrapEmail() {
        return bootstrapEmail;
    }

    public void setBootstrapEmail(String bootstrapEmail) {
        this.bootstrapEmail = bootstrapEmail;
    }

    public String getBootstrapPassword() {
        return bootstrapPassword;
    }

    public void setBootstrapPassword(String bootstrapPassword) {
        this.bootstrapPassword = bootstrapPassword;
    }

    public String getBootstrapDisplayName() {
        return bootstrapDisplayName;
    }

    public void setBootstrapDisplayName(String bootstrapDisplayName) {
        this.bootstrapDisplayName = bootstrapDisplayName;
    }

    public String getBootstrapTimezone() {
        return bootstrapTimezone;
    }

    public void setBootstrapTimezone(String bootstrapTimezone) {
        this.bootstrapTimezone = bootstrapTimezone;
    }
}
