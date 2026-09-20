package com.tennisplatform.availability.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvailabilityOverrideTest {

    private static final UUID TEACHER = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 5, 4);

    @Test
    void aBlockWithoutHoursCoversTheWholeDay() {
        AvailabilityOverride block = AvailabilityOverride.create(TEACHER, DATE, null,
                null, AvailabilityOverrideType.BLOCK);

        assertThat(block.blocksTheWholeDay()).isTrue();
        assertThat(block.hours()).isEmpty();
    }

    @Test
    void aBlockWithHoursCoversOnlyThoseHours() {
        AvailabilityOverride block = AvailabilityOverride.create(TEACHER, DATE,
                LocalTime.of(10, 0), LocalTime.of(12, 0), AvailabilityOverrideType.BLOCK);

        assertThat(block.blocksTheWholeDay()).isFalse();
        assertThat(block.hours()).contains(new LocalTimeRange(LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    /** "Extra availability, some time that day" is not something a calendar can draw. */
    @Test
    void extraAvailabilityNeedsBothTimes() {
        assertThatThrownBy(() -> AvailabilityOverride.create(TEACHER, DATE, null, null,
                AvailabilityOverrideType.EXTRA))
                .isInstanceOf(InvalidAvailabilityException.class);
        assertThatThrownBy(() -> AvailabilityOverride.create(TEACHER, DATE,
                LocalTime.of(18, 0), null, AvailabilityOverrideType.EXTRA))
                .isInstanceOf(InvalidAvailabilityException.class);
    }

    /** One time without the other describes nothing, whichever type it is. */
    @Test
    void halfAnIntervalIsRejectedOnABlockToo() {
        assertThatThrownBy(() -> AvailabilityOverride.create(TEACHER, DATE,
                LocalTime.of(10, 0), null, AvailabilityOverrideType.BLOCK))
                .isInstanceOf(InvalidAvailabilityException.class);
    }

    @Test
    void theTypeIsParsedByNameAndAnythingElseIsRejected() {
        assertThat(AvailabilityOverrideType.parse("block")).isEqualTo(AvailabilityOverrideType.BLOCK);
        assertThatThrownBy(() -> AvailabilityOverrideType.parse("HOLIDAY"))
                .isInstanceOf(InvalidAvailabilityException.class);
        assertThatThrownBy(() -> AvailabilityOverrideType.parse(null))
                .isInstanceOf(InvalidAvailabilityException.class);
    }
}
