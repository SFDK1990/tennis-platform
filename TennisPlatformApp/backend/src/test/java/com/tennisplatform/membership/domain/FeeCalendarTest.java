package com.tennisplatform.membership.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Which months are owed and when each falls due, on the teacher's wall clock (decision 3). */
class FeeCalendarTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final FeeCalendar CALENDAR = new FeeCalendar(MADRID, FeeSettings.DEFAULT);
    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);

    private static GroupMembership since(String joinedAt, String leftAt) {
        return new GroupMembership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse(joinedAt), leftAt == null ? null : Instant.parse(leftAt));
    }

    @Test
    void aMonthIsOwedForEachLessonWithinTheMembershipOnTheTeachersClock() {
        GroupMembership membership = since("2026-10-01T00:00:00Z", "2026-12-31T23:00:00Z");

        // 23:30 UTC on 31 October is already 1 November in Madrid; 1 January is after the end.
        assertThat(CALENDAR.owedMonths(membership, List.of(Instant.parse("2026-10-31T23:30:00Z"),
                Instant.parse("2026-11-10T17:00:00Z"), Instant.parse("2026-12-01T17:00:00Z"),
                Instant.parse("2027-01-05T17:00:00Z"), Instant.parse("2026-09-29T17:00:00Z"))))
                .containsExactly(NOVEMBER, YearMonth.of(2026, 12));
    }

    @Test
    void aMonthUnpaidIsPendingUntilItsDueDayAndOverdueAfter() {
        GroupMembership membership = since("2026-09-01T08:00:00Z", null);

        assertThat(CALENDAR.statusOf(NOVEMBER, membership, null, LocalDate.of(2026, 11, 5))).isEqualTo(FeeStatus.PENDING);
        assertThat(CALENDAR.statusOf(NOVEMBER, membership, null, LocalDate.of(2026, 11, 6))).isEqualTo(FeeStatus.OVERDUE);
    }

    /** Somebody who joins on the 20th is not overdue the moment they join. */
    @Test
    void aMonthJoinedAfterTheDueDayFallsDueOnTheDayOfJoining() {
        GroupMembership membership = since("2026-11-20T10:00:00Z", null);

        assertThat(CALENDAR.dueOn(NOVEMBER, membership)).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(CALENDAR.dueOn(NOVEMBER.plusMonths(1), membership)).isEqualTo(LocalDate.of(2026, 12, 5));
        assertThat(CALENDAR.statusOf(NOVEMBER, membership, null, LocalDate.of(2026, 11, 20))).isEqualTo(FeeStatus.PENDING);
    }

    @Test
    void aRecordedMonthIsPaidOrWaivedAndAVoidedOneCountsAsNothing() {
        GroupMembership membership = since("2026-09-01T08:00:00Z", null);
        Instant now = Instant.parse("2026-11-03T10:00:00Z");
        LocalDate late = LocalDate.of(2026, 12, 1);
        MonthlyFee paid = MonthlyFee.paid(membership.id(), NOVEMBER, 6_000, LocalDate.of(2026, 11, 3),
                FeePaymentMethod.CASH, now);
        MonthlyFee waived = MonthlyFee.waived(membership.id(), NOVEMBER, now);

        assertThat(CALENDAR.statusOf(NOVEMBER, membership, paid, late)).isEqualTo(FeeStatus.PAID);
        assertThat(CALENDAR.statusOf(NOVEMBER, membership, waived, late)).isEqualTo(FeeStatus.WAIVED);
        assertThat(CALENDAR.statusOf(NOVEMBER, membership, paid.voided(now), late)).isEqualTo(FeeStatus.OVERDUE);
    }
}
