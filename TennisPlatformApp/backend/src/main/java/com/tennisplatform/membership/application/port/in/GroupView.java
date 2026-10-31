package com.tennisplatform.membership.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A fixed group as the screens show it, worked out from the lessons of its series when read.
 *
 * <p>{@code levelId} is the level of its next lesson, null when that lesson is open to everybody
 * or there is none. {@code freeSeats} is how many more students could hold a seat in every lesson
 * still to come, null when none is left. {@code members} is filled for the teacher only: who is in
 * a group is not for the other students to see.
 */
public record GroupView(UUID id, String name, int monthlyFeeCents, List<SeriesView> series, UUID levelId,
                        Integer freeSeats, int memberCount, List<MemberView> members) {

    public GroupView {
        series = List.copyOf(series);
        members = List.copyOf(members);
    }

    public GroupView withoutMembers() {
        return new GroupView(id, name, monthlyFeeCents, series, levelId, freeSeats, memberCount, List.of());
    }

    /** One series of the group: its next lesson, if any is left, and how many are still to come. */
    public record SeriesView(UUID seriesId, String lessonType, Instant nextStartsAt, Instant nextEndsAt,
                             int upcomingLessons) {
    }

    /** {@code leftAt} in the future: the student is leaving at the end of the month. */
    public record MemberView(UUID membershipId, UUID studentUserId, Instant joinedAt, Instant leftAt) {
    }
}
