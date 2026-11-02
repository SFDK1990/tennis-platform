package com.tennisplatform.membership.application.service;

import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.membership.application.port.in.GroupView;
import com.tennisplatform.membership.application.port.in.GroupView.MemberView;
import com.tennisplatform.membership.application.port.in.GroupView.SeriesView;
import com.tennisplatform.membership.application.port.out.GroupSeriesRepository;
import com.tennisplatform.membership.domain.FixedGroup;
import com.tennisplatform.membership.domain.GroupMembership;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The lessons of each group, read from its series every time: a group is nothing but its series,
 * and what it is (days, times, level, seats) is what its lessons say.
 */
final class GroupLessons {

    private static final String CANCELLED = "CANCELLED";

    private final GroupSeriesRepository groupSeries;
    private final GetLesson lessons;

    GroupLessons(GroupSeriesRepository groupSeries, GetLesson lessons) {
        this.groupSeries = groupSeries;
        this.lessons = lessons;
    }

    /** The series of each group with their lessons, cancelled ones included. */
    Map<UUID, Map<UUID, List<LessonView>>> of(Collection<UUID> groupIds) {
        Map<UUID, List<UUID>> series = groupSeries.seriesOf(groupIds);
        Map<UUID, Map<UUID, List<LessonView>>> found = new HashMap<>();
        for (UUID groupId : groupIds) {
            Map<UUID, List<LessonView>> ofGroup = new HashMap<>();
            for (UUID seriesId : series.getOrDefault(groupId, List.of())) {
                ofGroup.put(seriesId, lessons.ofSeries(seriesId));
            }
            found.put(groupId, ofGroup);
        }
        return found;
    }

    Map<UUID, List<LessonView>> of(UUID groupId) {
        return of(List.of(groupId)).get(groupId);
    }

    /** The lessons of the group still to come and not cancelled, in the order they run. */
    static List<LessonView> upcoming(Map<UUID, List<LessonView>> series, Instant now) {
        return series.values().stream()
                .flatMap(List::stream)
                .filter(lesson -> !CANCELLED.equals(lesson.status()) && lesson.startsAt().isAfter(now))
                .sorted(Comparator.comparing(LessonView::startsAt))
                .toList();
    }

    /** When each lesson of the group that was not cancelled starts: the months a fee is owed for. */
    static List<Instant> starts(Map<UUID, List<LessonView>> series) {
        return series.values().stream()
                .flatMap(List::stream)
                .filter(lesson -> !CANCELLED.equals(lesson.status()))
                .map(LessonView::startsAt)
                .toList();
    }

    static GroupView view(FixedGroup group, Map<UUID, List<LessonView>> series, List<GroupMembership> memberships,
                          Instant now) {
        List<SeriesView> views = new ArrayList<>();
        series.forEach((seriesId, ofSeries) -> {
            List<LessonView> next = upcoming(Map.of(seriesId, ofSeries), now);
            String type = ofSeries.isEmpty() ? null : ofSeries.getFirst().type();
            views.add(next.isEmpty()
                    ? new SeriesView(seriesId, type, null, null, 0)
                    : new SeriesView(seriesId, type, next.getFirst().startsAt(), next.getFirst().endsAt(), next.size()));
        });
        views.sort(Comparator.comparing(SeriesView::nextStartsAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SeriesView::seriesId));

        List<LessonView> upcoming = upcoming(series, now);
        UUID levelId = upcoming.isEmpty() ? null : upcoming.getFirst().levelId();
        Integer freeSeats = upcoming.stream()
                .map(lesson -> Math.max(0, lesson.capacity() - lesson.bookedCount()))
                .min(Integer::compare)
                .orElse(null);
        List<MemberView> members = memberships.stream()
                .filter(membership -> membership.isInAt(now))
                .sorted(Comparator.comparing(GroupMembership::joinedAt))
                .map(membership -> new MemberView(membership.id(), membership.studentUserId(), membership.joinedAt(),
                        membership.leftAt()))
                .toList();
        return new GroupView(group.id(), group.name(), group.monthlyFeeCents(), views, levelId, freeSeats,
                members.size(), members);
    }
}
