package com.tennisplatform.membership.application.service;

import com.tennisplatform.booking.application.port.in.GroupBookings;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.membership.application.port.in.GroupView;
import com.tennisplatform.membership.application.port.in.ManageGroups;
import com.tennisplatform.membership.application.port.in.QueryGroups;
import com.tennisplatform.membership.application.port.out.FixedGroupRepository;
import com.tennisplatform.membership.application.port.out.GroupSeriesRepository;
import com.tennisplatform.membership.application.port.out.MembershipRepository;
import com.tennisplatform.membership.domain.FixedGroup;
import com.tennisplatform.membership.domain.GroupFullException;
import com.tennisplatform.membership.domain.GroupInUseException;
import com.tennisplatform.membership.domain.GroupMembership;
import com.tennisplatform.membership.domain.GroupNotFoundException;
import com.tennisplatform.membership.domain.SeriesNotFoundException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** The teacher's fixed groups and the series they are made of (decision 1). */
public class GroupsService implements ManageGroups, QueryGroups {

    private static final Logger log = LoggerFactory.getLogger(GroupsService.class);

    private final FixedGroupRepository groups;
    private final GroupSeriesRepository groupSeries;
    private final MembershipRepository memberships;
    private final GroupLessons groupLessons;
    private final GetLesson lessons;
    private final GroupBookings seats;
    private final GetTeacherProfile teacherProfile;
    private final Clock clock;

    public GroupsService(FixedGroupRepository groups, GroupSeriesRepository groupSeries,
                         MembershipRepository memberships, GetLesson lessons, GroupBookings seats,
                         GetTeacherProfile teacherProfile, Clock clock) {
        this.groups = groups;
        this.groupSeries = groupSeries;
        this.memberships = memberships;
        this.groupLessons = new GroupLessons(groupSeries, lessons);
        this.lessons = lessons;
        this.seats = seats;
        this.teacherProfile = teacherProfile;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupView> all() {
        return views(teacherProfile.get().userId()).stream().map(GroupView::withoutMembers).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupView> ofTeacher(UUID teacherUserId) {
        return views(teacherUserId);
    }

    @Override
    @Transactional
    public GroupView create(UUID teacherUserId, String name, int monthlyFeeCents) {
        FixedGroup saved = groups.save(FixedGroup.create(teacherUserId, name, monthlyFeeCents, clock.instant()));
        log.info("Group {} created", saved.id());
        return view(saved);
    }

    @Override
    @Transactional
    public GroupView update(UUID teacherUserId, UUID groupId, String name, Integer monthlyFeeCents) {
        FixedGroup saved = groups.save(owned(teacherUserId, groupId).changed(name, monthlyFeeCents));
        log.info("Group {} changed", groupId);
        return view(saved);
    }

    @Override
    @Transactional
    public void delete(UUID teacherUserId, UUID groupId) {
        owned(teacherUserId, groupId);
        if (memberships.existsInGroup(groupId)) {
            throw new GroupInUseException("Somebody has been signed up to this group, so it stays");
        }
        groups.delete(groupId);
        log.info("Group {} deleted", groupId);
    }

    @Override
    @Transactional
    public GroupView attachSeries(UUID teacherUserId, UUID groupId, UUID seriesId) {
        FixedGroup group = owned(teacherUserId, groupId);
        List<LessonView> ofSeries = lessons.ofSeries(seriesId);
        if (ofSeries.isEmpty() || !ofSeries.getFirst().teacherUserId().equals(teacherUserId)) {
            throw new SeriesNotFoundException("No weekly series with id " + seriesId);
        }
        groupSeries.attach(groupId, seriesId);

        // Every member gets a seat in the new lessons, or the series is not added at all.
        Instant now = clock.instant();
        List<LessonView> upcoming = GroupLessons.upcoming(Map.of(seriesId, ofSeries), now);
        for (GroupMembership member : memberships.findByGroup(groupId)) {
            if (!member.isInAt(now)) {
                continue;
            }
            List<UUID> theirs = upcoming.stream()
                    .filter(lesson -> member.covers(lesson.startsAt()))
                    .map(LessonView::id)
                    .toList();
            List<Instant> full = seats.bookSeats(teacherUserId, member.studentUserId(), member.id(), theirs);
            if (!full.isEmpty()) {
                throw new GroupFullException("Some lessons of the series have no seat for every member of the group",
                        full);
            }
        }
        log.info("Series {} added to group {}", seriesId, groupId);
        return view(group);
    }

    /** Another teacher's group answers like one that does not exist. */
    private FixedGroup owned(UUID teacherUserId, UUID groupId) {
        return groups.findById(groupId)
                .filter(group -> group.teacherUserId().equals(teacherUserId))
                .orElseThrow(() -> new GroupNotFoundException("No group with id " + groupId));
    }

    private GroupView view(FixedGroup group) {
        return GroupLessons.view(group, groupLessons.of(group.id()), memberships.findByGroup(group.id()),
                clock.instant());
    }

    private List<GroupView> views(UUID teacherUserId) {
        List<FixedGroup> found = groups.findByTeacher(teacherUserId);
        if (found.isEmpty()) {
            return List.of();
        }
        Map<UUID, Map<UUID, List<LessonView>>> series = groupLessons.of(found.stream().map(FixedGroup::id).toList());
        Map<UUID, List<GroupMembership>> members = memberships.findByTeacher(teacherUserId).stream()
                .collect(Collectors.groupingBy(GroupMembership::groupId));
        Instant now = clock.instant();
        return found.stream()
                .map(group -> GroupLessons.view(group, series.get(group.id()),
                        members.getOrDefault(group.id(), List.of()), now))
                .toList();
    }
}
