package com.tennisplatform.membership.application.service;

import com.tennisplatform.booking.application.port.in.GroupBookings;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.membership.application.port.in.FeeLineView;
import com.tennisplatform.membership.application.port.in.GroupView;
import com.tennisplatform.membership.application.port.in.GroupView.MemberView;
import com.tennisplatform.membership.application.port.in.JoinGroups;
import com.tennisplatform.membership.application.port.in.MembershipEnding;
import com.tennisplatform.membership.application.port.in.MembershipStarted;
import com.tennisplatform.membership.application.port.in.MembershipView;
import com.tennisplatform.membership.application.port.out.FeeSettingsRepository;
import com.tennisplatform.membership.application.port.out.FixedGroupRepository;
import com.tennisplatform.membership.application.port.out.GroupSeriesRepository;
import com.tennisplatform.membership.application.port.out.MembershipRepository;
import com.tennisplatform.membership.application.port.out.MonthlyFeeRepository;
import com.tennisplatform.membership.domain.AlreadyInGroupException;
import com.tennisplatform.membership.domain.EmailNotVerifiedException;
import com.tennisplatform.membership.domain.FixedGroup;
import com.tennisplatform.membership.domain.GroupFullException;
import com.tennisplatform.membership.domain.GroupMembership;
import com.tennisplatform.membership.domain.GroupNotFoundException;
import com.tennisplatform.membership.domain.NotInGroupException;
import com.tennisplatform.student.application.port.spi.StudentMemberships;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Signing up to a group and leaving it (decisions 2 and 5), and what letting a student go does to
 * their groups.
 *
 * <p>Joining is one transaction: the membership is written, then {@code booking} locks each lesson
 * and books the seats. When a lesson is full the whole of it is rolled back, the membership too, so
 * nobody is half in a group.
 */
public class MembershipsService implements JoinGroups, StudentMemberships {

    private static final Logger log = LoggerFactory.getLogger(MembershipsService.class);

    private final FixedGroupRepository groups;
    private final MembershipRepository memberships;
    private final GroupLessons groupLessons;
    private final FeeBook book;
    private final GroupBookings seats;
    private final GetTeacherProfile teacherProfile;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public MembershipsService(FixedGroupRepository groups, GroupSeriesRepository groupSeries,
                              MembershipRepository memberships, MonthlyFeeRepository fees,
                              FeeSettingsRepository settings, GetLesson lessons, GroupBookings seats,
                              GetTeacherProfile teacherProfile, ApplicationEventPublisher publisher, Clock clock) {
        this.groups = groups;
        this.memberships = memberships;
        this.groupLessons = new GroupLessons(groupSeries, lessons);
        this.book = new FeeBook(groups, fees, settings, groupLessons, teacherProfile);
        this.seats = seats;
        this.teacherProfile = teacherProfile;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MembershipView join(UUID studentUserId, boolean emailVerified, UUID groupId) {
        if (!emailVerified) {
            throw new EmailNotVerifiedException("Verify your email address before joining a group");
        }
        FixedGroup group = owned(teacherProfile.get().userId(), groupId);
        GroupMembership joined = start(group, studentUserId, true);
        Instant now = clock.instant();
        Map<UUID, List<LessonView>> series = groupLessons.of(groupId);
        return new MembershipView(joined.id(),
                GroupLessons.view(group, series, memberships.findByGroup(groupId), now).withoutMembers(),
                joined.joinedAt(), joined.leftAt(), book.lines(group.teacherUserId(), List.of(joined), null, now));
    }

    @Override
    @Transactional
    public void leave(UUID studentUserId, UUID groupId) {
        FixedGroup group = owned(teacherProfile.get().userId(), groupId);
        end(group, studentUserId, true);
    }

    @Override
    @Transactional
    public MemberView add(UUID teacherUserId, UUID groupId, UUID studentUserId) {
        GroupMembership joined = start(owned(teacherUserId, groupId), studentUserId, false);
        return new MemberView(joined.id(), joined.studentUserId(), joined.joinedAt(), joined.leftAt());
    }

    @Override
    @Transactional
    public void remove(UUID teacherUserId, UUID groupId, UUID studentUserId) {
        end(owned(teacherUserId, groupId), studentUserId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipView> ofStudent(UUID studentUserId) {
        List<GroupMembership> found = memberships.findByStudent(studentUserId).stream()
                .sorted(Comparator.comparing(GroupMembership::joinedAt).reversed())
                .toList();
        if (found.isEmpty()) {
            return List.of();
        }
        Instant now = clock.instant();
        UUID teacherUserId = found.getFirst().teacherUserId();
        Map<UUID, List<FeeLineView>> lines = book.lines(teacherUserId, found, null, now).stream()
                .collect(Collectors.groupingBy(FeeLineView::membershipId));
        Map<UUID, Map<UUID, List<LessonView>>> series = groupLessons.of(
                found.stream().map(GroupMembership::groupId).distinct().toList());
        return found.stream()
                .map(membership -> {
                    FixedGroup group = groups.findById(membership.groupId()).orElseThrow();
                    GroupView view = GroupLessons.view(group, series.get(group.id()),
                            memberships.findByGroup(group.id()), now).withoutMembers();
                    return new MembershipView(membership.id(), view, membership.joinedAt(), membership.leftAt(),
                            lines.getOrDefault(membership.id(), List.of()));
                })
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void endAllWith(UUID teacherUserId, UUID studentUserId, Instant at) {
        for (GroupMembership membership : memberships.findByStudent(studentUserId)) {
            if (membership.teacherUserId().equals(teacherUserId) && membership.isInAt(at)) {
                memberships.save(membership.endedAt(at));
                log.info("Membership {} ended with the student", membership.id());
            }
        }
    }

    /**
     * Writes the membership and books the seats of every lesson still to come. A student still in
     * the group, even one leaving at the end of the month, is already in.
     */
    private GroupMembership start(FixedGroup group, UUID studentUserId, boolean byStudent) {
        Instant now = clock.instant();
        boolean alreadyIn = memberships.findOfStudentInGroup(group.id(), studentUserId).stream()
                .anyMatch(membership -> membership.isInAt(now));
        if (alreadyIn) {
            throw new AlreadyInGroupException("The student is already in this group");
        }
        GroupMembership saved = memberships.save(GroupMembership.join(group, studentUserId, now));
        List<UUID> lessons = GroupLessons.upcoming(groupLessons.of(group.id()), now).stream()
                .map(LessonView::id)
                .toList();
        List<Instant> full = seats.bookSeats(group.teacherUserId(), studentUserId, saved.id(), lessons);
        if (!full.isEmpty()) {
            throw new GroupFullException("Some lessons of the group have no seat left", full);
        }
        log.info("Membership {} started in group {} for student {}", saved.id(), group.id(), studentUserId);
        publisher.publishEvent(new MembershipStarted(saved.id(), group.id(), group.teacherUserId(), studentUserId,
                byStudent));
        return saved;
    }

    /** The lessons of the month under way are kept; the rest are freed (decision 5). */
    private void end(FixedGroup group, UUID studentUserId, boolean byStudent) {
        Instant now = clock.instant();
        GroupMembership open = memberships.findOfStudentInGroup(group.id(), studentUserId).stream()
                .filter(GroupMembership::isOpen)
                .findFirst()
                .orElseThrow(() -> new NotInGroupException("The student is not in this group"));
        GroupMembership leaving = memberships.save(
                open.leavingAfterThisMonth(now, book.calendarOf(group.teacherUserId()).zone()));
        seats.cancelSeats(leaving.id(), leaving.leftAt(), byStudent);
        log.info("Membership {} ends at {}", leaving.id(), leaving.leftAt());
        publisher.publishEvent(new MembershipEnding(leaving.id(), group.id(), group.teacherUserId(), studentUserId,
                leaving.leftAt(), byStudent));
    }

    /** Another teacher's group answers like one that does not exist. */
    private FixedGroup owned(UUID teacherUserId, UUID groupId) {
        return groups.findById(groupId)
                .filter(group -> group.teacherUserId().equals(teacherUserId))
                .orElseThrow(() -> new GroupNotFoundException("No group with id " + groupId));
    }
}
