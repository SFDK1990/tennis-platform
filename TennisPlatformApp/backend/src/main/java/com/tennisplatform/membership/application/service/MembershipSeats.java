package com.tennisplatform.membership.application.service;

import com.tennisplatform.booking.application.port.spi.GroupSeats;
import com.tennisplatform.membership.application.port.out.GroupSeriesRepository;
import com.tennisplatform.membership.application.port.out.MembershipRepository;
import com.tennisplatform.membership.domain.GroupMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code booking}'s question of whether a seat is the group's. It reads this module's tables only,
 * so the booking beans that ask it do not wait on anything that asks them.
 */
public class MembershipSeats implements GroupSeats {

    private final GroupSeriesRepository groupSeries;
    private final MembershipRepository memberships;

    public MembershipSeats(GroupSeriesRepository groupSeries, MembershipRepository memberships) {
        this.groupSeries = groupSeries;
        this.memberships = memberships;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> membershipPaying(UUID studentUserId, UUID seriesId, Instant lessonStartsAt) {
        return groupSeries.groupOf(seriesId)
                .flatMap(groupId -> memberships.findOfStudentInGroup(groupId, studentUserId).stream()
                        .filter(membership -> membership.covers(lessonStartsAt))
                        .findFirst())
                .map(GroupMembership::id);
    }
}
