package com.tennisplatform.lesson.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Criterion of 35-fase22: the series keeps the wall-clock time across the change of time. */
class WeeklySeriesTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    /** Madrid leaves summer time on 25 October 2026: 18:00 is 16:00Z before and 17:00Z after. */
    @Test
    void keepsTheWallClockTimeAcrossTheChangeOfTime() {
        List<LessonPeriod> weeks = WeeklySeries.occurrences(Instant.parse("2026-10-20T16:00:00Z"),
                Instant.parse("2026-10-20T17:00:00Z"), MADRID, 3);

        assertThat(weeks).extracting(LessonPeriod::startsAt).containsExactly(
                Instant.parse("2026-10-20T16:00:00Z"),
                Instant.parse("2026-10-27T17:00:00Z"),
                Instant.parse("2026-11-03T17:00:00Z"));
        assertThat(weeks).extracting(LessonPeriod::endsAt).containsExactly(
                Instant.parse("2026-10-20T17:00:00Z"),
                Instant.parse("2026-10-27T18:00:00Z"),
                Instant.parse("2026-11-03T18:00:00Z"));
    }

    @Test
    void runsFromTwoToSixteenWeeks() {
        Instant start = Instant.parse("2026-11-03T17:00:00Z");
        Instant end = Instant.parse("2026-11-03T18:00:00Z");

        assertThat(WeeklySeries.occurrences(start, end, MADRID, 16)).hasSize(16);
        assertThatThrownBy(() -> WeeklySeries.occurrences(start, end, MADRID, 17))
                .isInstanceOf(InvalidLessonException.class);
        assertThatThrownBy(() -> WeeklySeries.occurrences(start, end, MADRID, 1))
                .isInstanceOf(InvalidLessonException.class);
    }
}
