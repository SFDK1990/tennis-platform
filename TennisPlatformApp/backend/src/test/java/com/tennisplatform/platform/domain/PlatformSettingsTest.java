package com.tennisplatform.platform.domain;

import org.junit.jupiter.api.Test;

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

    /** A cap of zero would make every group lesson impossible, which is not a configuration. */
    @Test
    void refusesAGroupCapacityThatWouldBlockEveryGroupLesson() {
        assertThatThrownBy(() -> PlatformSettings.of(50, 0))
                .isInstanceOf(InvalidPlatformSettingsException.class);
        assertThatThrownBy(() -> PlatformSettings.of(50, -1))
                .isInstanceOf(InvalidPlatformSettingsException.class);
    }
}
