package com.tennisplatform.membership.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Which months a membership owes and how each stands, on the teacher's calendar.
 *
 * <p>A month is owed when the group has a lesson in it, not cancelled, that belongs to the
 * membership. Cancelling one lesson, whoever does it, does not change the fee: the teacher records
 * less or waives the month if they want to make up for it.
 */
public final class FeeCalendar {

    private final ZoneId zone;
    private final FeeSettings settings;

    public FeeCalendar(ZoneId zone, FeeSettings settings) {
        this.zone = zone;
        this.settings = settings;
    }

    /** The months, in the teacher's zone, of the lessons that start within the membership. */
    public SortedSet<YearMonth> owedMonths(GroupMembership membership, Collection<Instant> lessonStarts) {
        SortedSet<YearMonth> months = new TreeSet<>();
        for (Instant startsAt : lessonStarts) {
            if (membership.covers(startsAt)) {
                months.add(YearMonth.from(startsAt.atZone(zone)));
            }
        }
        return months;
    }

    /**
     * The last day to pay a month: the due day, or the day the student joined when that was later
     * in the same month. Somebody who joins on the 20th is not overdue the moment they join.
     */
    public LocalDate dueOn(YearMonth month, GroupMembership membership) {
        LocalDate due = month.atDay(settings.dueDay());
        LocalDate joined = membership.joinedAt().atZone(zone).toLocalDate();
        return YearMonth.from(joined).equals(month) && joined.isAfter(due) ? joined : due;
    }

    /** {@code recorded} is the live fee of the month, or null when nothing was recorded. */
    public FeeStatus statusOf(YearMonth month, GroupMembership membership, MonthlyFee recorded, LocalDate today) {
        if (recorded != null && !recorded.isVoided()) {
            return recorded.kind() == FeeKind.PAID ? FeeStatus.PAID : FeeStatus.WAIVED;
        }
        return today.isAfter(dueOn(month, membership)) ? FeeStatus.OVERDUE : FeeStatus.PENDING;
    }

    public LocalDate today(Instant now) {
        return now.atZone(zone).toLocalDate();
    }

    public YearMonth monthOf(Instant instant) {
        return YearMonth.from(instant.atZone(zone));
    }

    public ZoneId zone() {
        return zone;
    }
}
