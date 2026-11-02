package com.tennisplatform.membership.application.service;

import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.membership.application.port.in.FeeLineView;
import com.tennisplatform.membership.application.port.in.FeeView;
import com.tennisplatform.membership.application.port.out.FeeSettingsRepository;
import com.tennisplatform.membership.application.port.out.FixedGroupRepository;
import com.tennisplatform.membership.application.port.out.MonthlyFeeRepository;
import com.tennisplatform.membership.domain.FeeCalendar;
import com.tennisplatform.membership.domain.FixedGroup;
import com.tennisplatform.membership.domain.GroupMembership;
import com.tennisplatform.membership.domain.MonthlyFee;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Works out the months of a set of memberships and how each stands, from the lessons of their
 * groups and the fees recorded. Nothing about a month is stored but what the teacher recorded.
 */
final class FeeBook {

    private final FixedGroupRepository groups;
    private final MonthlyFeeRepository fees;
    private final FeeSettingsRepository settings;
    private final GroupLessons groupLessons;
    private final GetTeacherProfile teacherProfile;

    FeeBook(FixedGroupRepository groups, MonthlyFeeRepository fees, FeeSettingsRepository settings,
            GroupLessons groupLessons, GetTeacherProfile teacherProfile) {
        this.groups = groups;
        this.fees = fees;
        this.settings = settings;
        this.groupLessons = groupLessons;
        this.teacherProfile = teacherProfile;
    }

    FeeCalendar calendarOf(UUID teacherUserId) {
        return new FeeCalendar(zoneOf(teacherUserId), settings.of(teacherUserId));
    }

    /**
     * The lines of {@code month}, or, with null, every month up to the current one plus any
     * recorded ahead: paying a month in advance is common.
     */
    List<FeeLineView> lines(UUID teacherUserId, Collection<GroupMembership> memberships, YearMonth month,
                            Instant now) {
        if (memberships.isEmpty()) {
            return List.of();
        }
        FeeCalendar calendar = calendarOf(teacherUserId);
        LocalDate today = calendar.today(now);
        YearMonth current = YearMonth.from(today);

        List<UUID> groupIds = memberships.stream().map(GroupMembership::groupId).distinct().toList();
        Map<UUID, FixedGroup> byId = groupIds.stream()
                .map(groups::findById)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(FixedGroup::id, Function.identity()));
        Map<UUID, Map<UUID, List<LessonView>>> lessons = groupLessons.of(groupIds);
        Map<UUID, Map<YearMonth, MonthlyFee>> recorded = fees.findLiveOf(
                        memberships.stream().map(GroupMembership::id).toList()).stream()
                .collect(Collectors.groupingBy(MonthlyFee::membershipId,
                        Collectors.toMap(MonthlyFee::month, Function.identity())));

        List<FeeLineView> lines = new ArrayList<>();
        for (GroupMembership membership : memberships) {
            FixedGroup group = byId.get(membership.groupId());
            Map<YearMonth, MonthlyFee> ofMembership = recorded.getOrDefault(membership.id(), Map.of());
            SortedSet<YearMonth> months = calendar.owedMonths(membership,
                    GroupLessons.starts(lessons.getOrDefault(membership.groupId(), Map.of())));
            months.addAll(ofMembership.keySet());
            for (YearMonth owed : months) {
                boolean wanted = month == null
                        ? !owed.isAfter(current) || ofMembership.containsKey(owed)
                        : owed.equals(month);
                if (!wanted) {
                    continue;
                }
                MonthlyFee fee = ofMembership.get(owed);
                lines.add(new FeeLineView(membership.id(), group.id(), group.name(), membership.studentUserId(),
                        owed.toString(), calendar.dueOn(owed, membership),
                        calendar.statusOf(owed, membership, fee, today).name(), group.monthlyFeeCents(),
                        fee == null ? null : FeeView.from(fee)));
            }
        }
        lines.sort(Comparator.comparing(FeeLineView::month).reversed()
                .thenComparing(FeeLineView::groupName)
                .thenComparing(line -> line.studentUserId().toString()));
        return lines;
    }

    private ZoneId zoneOf(UUID teacherUserId) {
        return ZoneId.of(teacherProfile.byUserId(teacherUserId).map(TeacherProfileView::timezone).orElseThrow());
    }
}
