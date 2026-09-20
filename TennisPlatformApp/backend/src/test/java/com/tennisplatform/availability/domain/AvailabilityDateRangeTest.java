package com.tennisplatform.availability.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvailabilityDateRangeTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    /** Criterion 10, in the domain: the cap is inclusive of both ends. */
    @Test
    void exactlyTheMaximumNumberOfDaysIsAccepted() {
        assertThatCode(() -> new AvailabilityDateRange(START, START.plusDays(
                AvailabilityDateRange.MAX_DAYS - 1))).doesNotThrowAnyException();
    }

    @Test
    void oneDayMoreIsRejected() {
        assertThatThrownBy(() -> new AvailabilityDateRange(START,
                START.plusDays(AvailabilityDateRange.MAX_DAYS)))
                .isInstanceOf(AvailabilityRangeTooWideException.class);
    }

    @Test
    void aSingleDayIsAValidRange() {
        assertThatCode(() -> new AvailabilityDateRange(START, START)).doesNotThrowAnyException();
    }

    @Test
    void aRangeThatEndsBeforeItStartsIsRejected() {
        assertThatThrownBy(() -> new AvailabilityDateRange(START, START.minusDays(1)))
                .isInstanceOf(InvalidAvailabilityException.class);
    }
}
