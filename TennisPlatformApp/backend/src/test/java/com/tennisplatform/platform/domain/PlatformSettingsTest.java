package com.tennisplatform.platform.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformSettingsTest {

    @Test
    void keepsTheConfiguredLimits() {
        PlatformSettings settings = PlatformSettings.of(25, 6);

        assertThat(settings.studentLimit()).isEqualTo(25);
        assertThat(settings.maxGroupCapacity()).isEqualTo(6);
    }

    /**
     * The same rule the CHECK constraints state. Having it here too is what lets the
     * application refuse a bad value with an explanation instead of a constraint violation.
     */
    @Test
    void refusesAStudentLimitThatWouldBlockEverybody() {
        assertThatThrownBy(() -> PlatformSettings.of(0, 8))
                .isInstanceOf(InvalidPlatformSettingsException.class);
        assertThatThrownBy(() -> PlatformSettings.of(-1, 8))
                .isInstanceOf(InvalidPlatformSettingsException.class);
    }

    @Test
    void aChangeKeepsWhatWasNotSentAndRecordsWhoAndWhen() {
        UUID admin = UUID.randomUUID();
        Instant at = Instant.parse("2026-09-26T10:00:00Z");

        PlatformSettings changed = PlatformSettings.of(25, 6).changedBy(admin, at, 10, null);

        assertThat(changed.studentLimit()).isEqualTo(10);
        assertThat(changed.maxGroupCapacity()).isEqualTo(6);
        assertThat(changed.updatedBy()).isEqualTo(admin);
        assertThat(changed.updatedAt()).isEqualTo(at);
    }

    @Test
    void aChangeFollowsTheSameRules() {
        PlatformSettings settings = PlatformSettings.of(25, 6);

        assertThatThrownBy(() -> settings.changedBy(UUID.randomUUID(), Instant.now(), 0, null))
                .isInstanceOf(InvalidPlatformSettingsException.class);
    }

    /** A cap of zero would make every group lesson impossible, which is not a configuration. */
    @Test
    void refusesAGroupCapacityThatWouldBlockEveryGroupLesson() {
        assertThatThrownBy(() -> PlatformSettings.of(50, 0))
                .isInstanceOf(InvalidPlatformSettingsException.class);
        assertThatThrownBy(() -> PlatformSettings.of(50, -1))
                .isInstanceOf(InvalidPlatformSettingsException.class);
    }
}
