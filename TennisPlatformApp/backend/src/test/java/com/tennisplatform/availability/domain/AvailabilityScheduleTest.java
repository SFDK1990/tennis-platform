package com.tennisplatform.availability.domain;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolution of weekly rules and date exceptions into real time.
 *
 * <p>The transition dates are written out rather than computed. A test that asks the same
 * library it is testing when the clocks change agrees with itself by construction and proves
 * nothing; these are the real dates for Europe/Madrid in 2026, looked up once.
 */
class AvailabilityScheduleTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final UUID TEACHER = UUID.randomUUID();

    /** The last Sunday of March 2026: 02:00 does not happen, the clock jumps to 03:00. */
    private static final LocalDate CLOCKS_GO_FORWARD = LocalDate.of(2026, 3, 29);

    /** The last Sunday of October 2026: 02:00 to 03:00 happens twice. */
    private static final LocalDate CLOCKS_GO_BACK = LocalDate.of(2026, 10, 25);

    /** Criterion 1: the same wall clock is a different instant in winter and in summer. */
    @Test
    void theSameRuleResolvesToADifferentInstantInWinterAndInSummer() {
        AvailabilitySchedule schedule = scheduleOf(rule(DayOfWeek.MONDAY, 9, 0, 13, 0));

        LocalDate winterMonday = LocalDate.of(2026, 1, 5);
        LocalDate summerMonday = LocalDate.of(2026, 7, 6);

        // CET is UTC+1, so 09:00 local is 08:00Z.
        assertThat(schedule.resolve(winterMonday, winterMonday))
                .containsExactly(new AvailabilityInterval(
                        Instant.parse("2026-01-05T08:00:00Z"), Instant.parse("2026-01-05T12:00:00Z")));

        // CEST is UTC+2, so the very same rule starts an hour earlier in UTC.
        assertThat(schedule.resolve(summerMonday, summerMonday))
                .containsExactly(new AvailabilityInterval(
                        Instant.parse("2026-07-06T07:00:00Z"), Instant.parse("2026-07-06T11:00:00Z")));
    }

    /**
     * Criterion 2: on the day the clocks go forward, a rule spanning the gap is one hour shorter
     * in real time than its wall clock says. Three hours on paper, two in the world.
     */
    @Test
    void aRuleSpanningTheSpringGapLosesAnHourOfRealTime() {
        AvailabilitySchedule schedule = scheduleOf(rule(DayOfWeek.SUNDAY, 1, 0, 4, 0));

        List<AvailabilityInterval> resolved = schedule.resolve(CLOCKS_GO_FORWARD, CLOCKS_GO_FORWARD);

        assertThat(resolved).containsExactly(new AvailabilityInterval(
                Instant.parse("2026-03-29T00:00:00Z"), Instant.parse("2026-03-29T02:00:00Z")));
        assertThat(java.time.Duration.between(resolved.get(0).startsAt(), resolved.get(0).endsAt()))
                .isEqualTo(java.time.Duration.ofHours(2));
    }

    /**
     * A rule that falls entirely inside the gap has both ends resolve to the same instant. It
     * must disappear rather than blow up: a read of the calendar cannot answer 500 because of
     * how the year is shaped.
     */
    @Test
    void aRuleThatFallsEntirelyInsideTheSpringGapSimplyDisappears() {
        AvailabilitySchedule schedule = scheduleOf(rule(DayOfWeek.SUNDAY, 2, 0, 3, 0));

        assertThat(schedule.resolve(CLOCKS_GO_FORWARD, CLOCKS_GO_FORWARD)).isEmpty();
    }

    /**
     * Criterion 3: on the day the clocks go back, a wall-clock time that happens twice takes the
     * first of the two. 02:00 resolves to 00:00Z (CEST); the later offset would give 01:00Z.
     */
    @Test
    void anHourThatHappensTwiceResolvesToTheFirstOfTheTwo() {
        AvailabilitySchedule schedule = scheduleOf(rule(DayOfWeek.SUNDAY, 2, 0, 5, 0));

        List<AvailabilityInterval> resolved = schedule.resolve(CLOCKS_GO_BACK, CLOCKS_GO_BACK);

        assertThat(resolved).containsExactly(new AvailabilityInterval(
                Instant.parse("2026-10-25T00:00:00Z"), Instant.parse("2026-10-25T04:00:00Z")));
        // Four real hours out of three on the wall clock: the repeated hour is lived through.
        assertThat(java.time.Duration.between(resolved.get(0).startsAt(), resolved.get(0).endsAt()))
                .isEqualTo(java.time.Duration.ofHours(4));
    }

    /** Criterion 6: a whole-day block wins over both the weekly rules and an extra. */
    @Test
    void aWholeDayBlockBeatsTheWeeklyRulesAndAnyExtraOnThatDate() {
        LocalDate monday = LocalDate.of(2026, 1, 5);
        AvailabilitySchedule schedule = AvailabilitySchedule.of(
                List.of(rule(DayOfWeek.MONDAY, 9, 0, 13, 0)),
                List.of(wholeDayBlock(monday), extra(monday, 18, 0, 20, 0)),
                MADRID);

        assertThat(schedule.resolve(monday, monday)).isEmpty();
    }

    /** Criterion 7: an extra outside the weekly hours shows up as available. */
    @Test
    void anExtraOutsideTheWeeklyHoursBecomesAvailable() {
        LocalDate monday = LocalDate.of(2026, 1, 5);
        AvailabilitySchedule schedule = AvailabilitySchedule.of(
                List.of(rule(DayOfWeek.MONDAY, 9, 0, 13, 0)),
                List.of(extra(monday, 18, 0, 20, 0)),
                MADRID);

        assertThat(schedule.availableHoursOn(monday)).containsExactly(
                new LocalTimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0)),
                new LocalTimeRange(LocalTime.of(18, 0), LocalTime.of(20, 0)));
    }

    /** A partial block cuts a hole in the middle of the weekly hours rather than removing them. */
    @Test
    void aPartialBlockSplitsTheWeeklyHoursInTwo() {
        LocalDate monday = LocalDate.of(2026, 1, 5);
        AvailabilitySchedule schedule = AvailabilitySchedule.of(
                List.of(rule(DayOfWeek.MONDAY, 9, 0, 13, 0)),
                List.of(block(monday, 10, 0, 11, 0)),
                MADRID);

        assertThat(schedule.availableHoursOn(monday)).containsExactly(
                new LocalTimeRange(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new LocalTimeRange(LocalTime.of(11, 0), LocalTime.of(13, 0)));
    }

    /** Criterion 9: a rule whose active period has passed says nothing about today. */
    @Test
    void aRuleThatStoppedBeingActiveDoesNotApply() {
        LocalDate monday = LocalDate.of(2026, 1, 5);
        WeeklyAvailabilityRule expired = WeeklyAvailabilityRule.create(TEACHER, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(13, 0), null, LocalDate.of(2025, 12, 31));

        assertThat(scheduleOf(expired).resolve(monday, monday)).isEmpty();
    }

    @Test
    void aRuleThatIsNotActiveYetDoesNotApplyEither() {
        LocalDate monday = LocalDate.of(2026, 1, 5);
        WeeklyAvailabilityRule future = WeeklyAvailabilityRule.create(TEACHER, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(13, 0), LocalDate.of(2026, 2, 1), null);

        assertThat(scheduleOf(future).resolve(monday, monday)).isEmpty();
    }

    @Test
    void coversAnswersForAnIntervalInsideTheWorkingHoursAndRefusesOneThatRunsPastThem() {
        AvailabilitySchedule schedule = scheduleOf(rule(DayOfWeek.MONDAY, 9, 0, 13, 0));

        assertThat(schedule.covers(Instant.parse("2026-01-05T09:00:00Z"),
                Instant.parse("2026-01-05T10:00:00Z"))).isTrue();
        assertThat(schedule.covers(Instant.parse("2026-01-05T11:30:00Z"),
                Instant.parse("2026-01-05T12:30:00Z"))).isFalse();
    }

    /** The bounds belong to the interval: a class filling the whole morning still fits. */
    @Test
    void coversAcceptsAnIntervalThatExactlyFillsTheAvailability() {
        AvailabilitySchedule schedule = scheduleOf(rule(DayOfWeek.MONDAY, 9, 0, 13, 0));

        assertThat(schedule.covers(Instant.parse("2026-01-05T08:00:00Z"),
                Instant.parse("2026-01-05T12:00:00Z"))).isTrue();
    }

    // --- helpers ---------------------------------------------------------------

    private static AvailabilitySchedule scheduleOf(WeeklyAvailabilityRule... rules) {
        return AvailabilitySchedule.of(List.of(rules), List.of(), MADRID);
    }

    private static WeeklyAvailabilityRule rule(DayOfWeek day, int startHour, int startMinute,
                                               int endHour, int endMinute) {
        return WeeklyAvailabilityRule.create(TEACHER, day, LocalTime.of(startHour, startMinute),
                LocalTime.of(endHour, endMinute), null, null);
    }

    private static AvailabilityOverride wholeDayBlock(LocalDate date) {
        return AvailabilityOverride.create(TEACHER, date, null, null,
                AvailabilityOverrideType.BLOCK);
    }

    private static AvailabilityOverride block(LocalDate date, int startHour, int startMinute,
                                                   int endHour, int endMinute) {
        return AvailabilityOverride.create(TEACHER, date, LocalTime.of(startHour, startMinute),
                LocalTime.of(endHour, endMinute), AvailabilityOverrideType.BLOCK);
    }

    private static AvailabilityOverride extra(LocalDate date, int startHour, int startMinute,
                                                   int endHour, int endMinute) {
        return AvailabilityOverride.create(TEACHER, date, LocalTime.of(startHour, startMinute),
                LocalTime.of(endHour, endMinute), AvailabilityOverrideType.EXTRA);
    }
}
