package com.tennisplatform.platform.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformSettingsTest {

    @Test
    void keepsTheConfiguredLimit() {
        assertThat(PlatformSettings.of(25).studentLimit()).isEqualTo(25);
    }

    /**
     * The same rule the CHECK constraint states. Having it here too is what lets the
     * application refuse a bad value with an explanation instead of a constraint violation.
     */
    @Test
    void refusesALimitThatWouldBlockEverybody() {
        assertThatThrownBy(() -> PlatformSettings.of(0))
                .isInstanceOf(InvalidPlatformSettingsException.class);
        assertThatThrownBy(() -> PlatformSettings.of(-1))
                .isInstanceOf(InvalidPlatformSettingsException.class);
    }
}
