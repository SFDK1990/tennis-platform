package com.tennisplatform.shared.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);

    /** Both ends count, so 62 days end on the 61st day after the first. */
    @Test
    void acceptsExactlySixtyTwoDaysAndRefusesSixtyThree() {
        assertThatCode(() -> new DateRange(FROM, FROM.plusDays(61))).doesNotThrowAnyException();
        assertThatThrownBy(() -> new DateRange(FROM, FROM.plusDays(62)))
                .isInstanceOf(DateRange.InvalidDateRangeException.class);
    }

    @Test
    void aSingleDayIsARange() {
        assertThatCode(() -> new DateRange(FROM, FROM)).doesNotThrowAnyException();
    }

    @Test
    void refusesAnEndBeforeTheStartAndAMissingEnd() {
        assertThatThrownBy(() -> new DateRange(FROM, FROM.minusDays(1)))
                .isInstanceOf(DateRange.InvalidDateRangeException.class);
        assertThatThrownBy(() -> new DateRange(FROM, null))
                .isInstanceOf(DateRange.InvalidDateRangeException.class);
    }
}
