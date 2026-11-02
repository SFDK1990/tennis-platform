package com.tennisplatform.membership.domain;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

/**
 * A student signed up to a fixed group, from {@code joinedAt} until {@code leftAt}, which may lie in
 * the future: leaving frees the lessons from the next month on (decision 5), and until then the
 * student is still in. A lesson belongs to the membership when it starts within that period.
 */
public record GroupMembership(UUID id, UUID groupId, UUID teacherUserId, UUID studentUserId, Instant joinedAt,
                              Instant leftAt) {

    public static GroupMembership join(FixedGroup group, UUID studentUserId, Instant now) {
        return new GroupMembership(null, group.id(), group.teacherUserId(), studentUserId, now, null);
    }

    /** Whether the student has not left, nor said they are leaving. */
    public boolean isOpen() {
        return leftAt == null;
    }

    /** Whether the student is still in at {@code at}: also true while a leave has not taken effect. */
    public boolean isInAt(Instant at) {
        return leftAt == null || at.isBefore(leftAt);
    }

    /** Whether a lesson that starts at {@code lessonStartsAt} is one of the membership's. */
    public boolean covers(Instant lessonStartsAt) {
        return !lessonStartsAt.isBefore(joinedAt) && isInAt(lessonStartsAt);
    }

    /**
     * Leaving, on the student's word or the teacher's: in at the start of the next month in the
     * teacher's zone, so the month being paid or owed keeps its lessons (decision 5).
     */
    public GroupMembership leavingAfterThisMonth(Instant now, ZoneId zone) {
        if (!isOpen()) {
            throw new NotInGroupException("The student is already leaving this group");
        }
        Instant nextMonth = YearMonth.from(now.atZone(zone)).plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        return new GroupMembership(id, groupId, teacherUserId, studentUserId, joinedAt, nextMonth);
    }

    /**
     * Ending at once, because the student stopped being the teacher's or their account went
     * (01-analisis-funcional.md §18): there are no lessons left to keep.
     */
    public GroupMembership endedAt(Instant now) {
        Instant end = now.isBefore(joinedAt) ? joinedAt : now;
        if (leftAt != null && !leftAt.isAfter(end)) {
            return this;
        }
        return new GroupMembership(id, groupId, teacherUserId, studentUserId, joinedAt, end);
    }
}
