package com.tennisplatform.lesson.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LessonDateRangeTest {

    private static final LocalDate FIRST = LocalDate.of(2026, 6, 1);

    @Test
    void acceptsASingleDay() {
        assertThat(new LessonDateRange(FIRST, FIRST).from()).isEqualTo(FIRST);
    }

    @Test
    void refusesARangeThatEndsBeforeItStarts() {
        assertThatThrownBy(() -> new LessonDateRange(FIRST, FIRST.minusDays(1)))
                .isInstanceOf(InvalidLessonException.class);
    }

    @Test
    void refusesAMissingBound() {
        assertThatThrownBy(() -> new LessonDateRange(FIRST, null))
                .isInstanceOf(InvalidLessonException.class);
        assertThatThrownBy(() -> new LessonDateRange(null, FIRST))
                .isInstanceOf(InvalidLessonException.class);
    }

    /** Both bounds count, so the widest allowed range ends MAX_DAYS - 1 days after it starts. */
    @Test
    void acceptsExactlyTheWidestAllowedRangeAndRefusesOneDayMore() {
        assertThatCode(() -> new LessonDateRange(FIRST, FIRST.plusDays(LessonDateRange.MAX_DAYS - 1)))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> new LessonDateRange(FIRST, FIRST.plusDays(LessonDateRange.MAX_DAYS)))
                .isInstanceOf(LessonRangeTooWideException.class);
    }

    /** The same cap the availability read uses, on purpose: two different numbers would be a difference nobody decided. */
    @Test
    void capsTheRangeAtTheSameNumberOfDaysAsTheAvailabilityRead() {
        assertThat(LessonDateRange.MAX_DAYS).isEqualTo(62);
    }
}
