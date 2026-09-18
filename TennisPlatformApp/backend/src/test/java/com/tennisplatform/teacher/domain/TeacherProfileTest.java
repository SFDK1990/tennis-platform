package com.tennisplatform.teacher.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeacherProfileTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void trimsTheDisplayNameAndKeepsTheTimeZone() {
        TeacherProfile profile = profileWith("  Ana Serrano  ", "Europe/Madrid");

        assertThat(profile.displayName()).isEqualTo("Ana Serrano");
        assertThat(profile.timezone().getId()).isEqualTo("Europe/Madrid");
    }

    @Test
    void refusesABlankDisplayName() {
        assertThatThrownBy(() -> profileWith("   ", "Europe/Madrid"))
                .isInstanceOf(InvalidTeacherProfileException.class)
                .hasMessageContaining("display name is required");
    }

    /**
     * The zone is the reference for every lesson shown in local time and for the rule about not
     * crossing midnight, so it has to be rejected here rather than the day a lesson is created.
     */
    @Test
    void refusesATimeZoneThatIsNotAnIanaId() {
        assertThatThrownBy(() -> profileWith("Ana", "Madrid/Spain"))
                .isInstanceOf(InvalidTeacherProfileException.class);

        assertThatThrownBy(() -> profileWith("Ana", "CET+3"))
                .isInstanceOf(InvalidTeacherProfileException.class);
    }

    @Test
    void aNullFieldLeavesThatValueUntouched() {
        TeacherProfile profile = profileWith("Ana", "Europe/Madrid");
        profile.update(null, "600123123", null);

        profile.update(null, null, "America/Bogota");

        assertThat(profile.displayName()).isEqualTo("Ana");
        assertThat(profile.phone()).isEqualTo("600123123");
        assertThat(profile.timezone().getId()).isEqualTo("America/Bogota");
    }

    /** A nullable field needs a way to be emptied on purpose, and PATCH uses null for "absent". */
    @Test
    void anEmptyPhoneClearsIt() {
        TeacherProfile profile = profileWith("Ana", "Europe/Madrid");
        profile.update(null, "600123123", null);

        profile.update(null, "", null);

        assertThat(profile.phone()).isNull();
    }

    @Test
    void refusesToUpdateToAnInvalidTimeZoneAndKeepsThePreviousOne() {
        TeacherProfile profile = profileWith("Ana", "Europe/Madrid");

        assertThatThrownBy(() -> profile.update(null, null, "Nowhere/Nothing"))
                .isInstanceOf(InvalidTeacherProfileException.class);
        assertThat(profile.timezone().getId()).isEqualTo("Europe/Madrid");
    }

    private TeacherProfile profileWith(String displayName, String timezone) {
        return TeacherProfile.create(USER_ID, displayName, null, timezone, NOW);
    }
}
