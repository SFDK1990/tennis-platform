package com.tennisplatform.availability.domain;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeeklyAvailabilityRuleTest {

    private static final UUID TEACHER = UUID.randomUUID();

    @Test
    void aRuleMustEndAfterItStarts() {
        assertThatThrownBy(() -> rule(DayOfWeek.MONDAY, 13, 0, 9, 0, null, null))
                .isInstanceOf(InvalidAvailabilityException.class);
        assertThatThrownBy(() -> rule(DayOfWeek.MONDAY, 9, 0, 9, 0, null, null))
                .isInstanceOf(InvalidAvailabilityException.class);
    }

    @Test
    void aRuleCannotStopBeingActiveBeforeItStarts() {
        assertThatThrownBy(() -> rule(DayOfWeek.MONDAY, 9, 0, 13, 0,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1)))
                .isInstanceOf(InvalidAvailabilityException.class);
    }

    /**
     * Criterion 5, in the domain: touching is not overlapping. A morning rule that ends when the
     * afternoon one begins is an ordinary schedule, and rejecting it would make the product
     * unusable while looking like carefulness.
     */
    @Test
    void adjacentRulesOnTheSameDayDoNotConflict() {
        WeeklyAvailabilityRule morning = rule(DayOfWeek.MONDAY, 9, 0, 11, 0, null, null);
        WeeklyAvailabilityRule afternoon = rule(DayOfWeek.MONDAY, 11, 0, 13, 0, null, null);

        assertThat(morning.conflictsWith(afternoon)).isFalse();
        assertThat(afternoon.conflictsWith(morning)).isFalse();
    }

    @Test
    void rulesThatShareAnHourOnTheSameDayConflict() {
        assertThat(rule(DayOfWeek.MONDAY, 9, 0, 13, 0, null, null)
                .conflictsWith(rule(DayOfWeek.MONDAY, 12, 0, 15, 0, null, null))).isTrue();
    }

    @Test
    void theSameHoursOnDifferentDaysNeverConflict() {
        assertThat(rule(DayOfWeek.MONDAY, 9, 0, 13, 0, null, null)
                .conflictsWith(rule(DayOfWeek.TUESDAY, 9, 0, 13, 0, null, null))).isFalse();
    }

    /**
     * The same hours on the same weekday are fine when one rule has stopped applying before the
     * other starts. That is precisely how a change of schedule is expressed, so treating it as a
     * conflict would leave no way to plan one.
     */
    @Test
    void theSameHoursDoNotConflictWhenTheirActivePeriodsDoNotMeet() {
        WeeklyAvailabilityRule untilFebruary = rule(DayOfWeek.MONDAY, 9, 0, 13, 0,
                null, LocalDate.of(2026, 2, 28));
        WeeklyAvailabilityRule fromMarch = rule(DayOfWeek.MONDAY, 9, 0, 13, 0,
                LocalDate.of(2026, 3, 1), null);

        assertThat(untilFebruary.conflictsWith(fromMarch)).isFalse();
    }

    @Test
    void aRuleAppliesOnlyOnItsOwnWeekdayAndInsideItsActivePeriod() {
        WeeklyAvailabilityRule inFebruary = rule(DayOfWeek.MONDAY, 9, 0, 13, 0,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

        assertThat(inFebruary.appliesOn(LocalDate.of(2026, 2, 2))).isTrue();
        assertThat(inFebruary.appliesOn(LocalDate.of(2026, 2, 3))).isFalse();
        assertThat(inFebruary.appliesOn(LocalDate.of(2026, 1, 26))).isFalse();
        assertThat(inFebruary.appliesOn(LocalDate.of(2026, 3, 2))).isFalse();
    }

    /** The active period includes both ends: a rule active "until the 28th" works on the 28th. */
    @Test
    void theActivePeriodIncludesBothOfItsEnds() {
        WeeklyAvailabilityRule bounded = rule(DayOfWeek.MONDAY, 9, 0, 13, 0,
                LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 23));

        assertThat(bounded.appliesOn(LocalDate.of(2026, 2, 2))).isTrue();
        assertThat(bounded.appliesOn(LocalDate.of(2026, 2, 23))).isTrue();
    }

    @Test
    void theWeekdayIsParsedByNameAndAnythingElseIsRejected() {
        assertThat(WeeklyAvailabilityRule.parseDayOfWeek("monday")).isEqualTo(DayOfWeek.MONDAY);
        assertThat(WeeklyAvailabilityRule.parseDayOfWeek(" SUNDAY ")).isEqualTo(DayOfWeek.SUNDAY);

        // A number is what the three competing conventions disagree about, so it is not accepted
        // at all rather than interpreted as one of them.
        assertThatThrownBy(() -> WeeklyAvailabilityRule.parseDayOfWeek("0"))
                .isInstanceOf(InvalidAvailabilityException.class);
        assertThatThrownBy(() -> WeeklyAvailabilityRule.parseDayOfWeek("lunes"))
                .isInstanceOf(InvalidAvailabilityException.class);
    }

    private static WeeklyAvailabilityRule rule(DayOfWeek day, int startHour, int startMinute,
                                               int endHour, int endMinute,
                                               LocalDate activeFrom, LocalDate activeUntil) {
        return WeeklyAvailabilityRule.create(TEACHER, day, LocalTime.of(startHour, startMinute),
                LocalTime.of(endHour, endMinute), activeFrom, activeUntil);
    }
}
