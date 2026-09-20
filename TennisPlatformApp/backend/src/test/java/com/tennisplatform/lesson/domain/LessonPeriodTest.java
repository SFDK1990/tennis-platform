package com.tennisplatform.lesson.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * When a lesson happens, and the two rules about it that are not about the calendar: the
 * duration, and whether it runs past midnight.
 *
 * <p>The daylight-saving dates are written out rather than computed. A test that asks the same
 * library it is testing when the clocks change agrees with itself by construction; these are the
 * real dates for Europe/Madrid in 2026, looked up once - the same ones
 * {@code AvailabilityScheduleTest} uses.
 */
class LessonPeriodTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    /** The last Sunday of March 2026: 02:00 does not happen, the clock jumps to 03:00. */
    private static final LocalDate CLOCKS_GO_FORWARD = LocalDate.of(2026, 3, 29);

    /** The last Sunday of October 2026: 02:00 to 03:00 happens twice. */
    private static final LocalDate CLOCKS_GO_BACK = LocalDate.of(2026, 10, 25);

    @Test
    void acceptsAWholeNumberOfHalfHours() {
        assertThat(period(10, 0, 10, 30).duration().toMinutes()).isEqualTo(30);
        assertThat(period(10, 0, 11, 0).duration().toMinutes()).isEqualTo(60);
        assertThat(period(10, 0, 11, 30).duration().toMinutes()).isEqualTo(90);
    }

    @Test
    void refusesADurationThatIsNotAWholeNumberOfHalfHours() {
        assertThatThrownBy(() -> period(10, 0, 10, 45))
                .isInstanceOf(InvalidLessonException.class);
        assertThatThrownBy(() -> period(10, 0, 10, 20))
                .isInstanceOf(InvalidLessonException.class);
    }

    /**
     * The 30-minute minimum needs no rule of its own: a lesson that starts before it ends and
     * lasts a whole number of half hours already lasts at least one.
     */
    @Test
    void refusesALessonThatEndsBeforeItStartsOrLastsNothing() {
        assertThatThrownBy(() -> period(11, 0, 10, 0))
                .isInstanceOf(InvalidLessonException.class);
        assertThatThrownBy(() -> period(10, 0, 10, 0))
                .isInstanceOf(InvalidLessonException.class);
    }

    @Test
    void refusesAMissingEnd() {
        assertThatThrownBy(() -> new LessonPeriod(Instant.parse("2026-06-01T10:00:00Z"), null))
                .isInstanceOf(InvalidLessonException.class);
    }

    @Test
    void crossesMidnightWhenItRunsIntoTheNextLocalDay() {
        assertThat(period(23, 0, 24, 30).crossesMidnightIn(MADRID)).isTrue();
        assertThat(period(22, 0, 23, 30).crossesMidnightIn(MADRID)).isFalse();
    }

    /** A lesson that stops exactly at midnight reaches the boundary without crossing it. */
    @Test
    void doesNotCrossMidnightWhenItEndsExactlyAtMidnight() {
        assertThat(period(23, 0, 24, 0).crossesMidnightIn(MADRID)).isFalse();
    }

    /**
     * Midnight is a local idea, so the very same two instants cross it in one zone and not in
     * another. This is the whole reason the rule cannot be a CHECK on the columns.
     */
    @Test
    void theSameInstantsCrossMidnightInOneZoneAndNotInAnother() {
        LessonPeriod lateInMadrid = period(23, 0, 24, 30);

        assertThat(lateInMadrid.crossesMidnightIn(MADRID)).isTrue();
        assertThat(lateInMadrid.crossesMidnightIn(NEW_YORK)).isFalse();
    }

    /**
     * On the day the clocks go forward, the teacher's own clock says two hours and only one
     * happened. The instant is what counts, and the duration rule is applied to it.
     */
    @Test
    void aLessonAcrossTheSpringTransitionLastsWhatTheClockActuallyRan() {
        Instant start = at(CLOCKS_GO_FORWARD, 1, 30);
        Instant end = at(CLOCKS_GO_FORWARD, 3, 30);

        assertThat(new LessonPeriod(start, end).duration().toMinutes()).isEqualTo(60);
    }

    /** And on the day they go back, one hour on the clock is two in real time. */
    @Test
    void aLessonAcrossTheAutumnTransitionLastsWhatTheClockActuallyRan() {
        Instant start = at(CLOCKS_GO_BACK, 1, 30);
        Instant end = at(CLOCKS_GO_BACK, 3, 30);

        assertThat(new LessonPeriod(start, end).duration().toMinutes()).isEqualTo(180);
    }

    private static LessonPeriod period(int startHour, int startMinute, int endHour, int endMinute) {
        LocalDate day = LocalDate.of(2026, 6, 1);
        return new LessonPeriod(at(day, startHour, startMinute), at(day, endHour, endMinute));
    }

    /** Hours beyond 24 roll into the next day, which is how the midnight cases are written. */
    private static Instant at(LocalDate day, int hour, int minute) {
        return LocalDateTime.of(day, java.time.LocalTime.MIDNIGHT)
                .plusHours(hour).plusMinutes(minute)
                .atZone(MADRID).toInstant();
    }
}
