package com.tennisplatform.membership.application.service;

import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.membership.application.port.in.FeeLineView;
import com.tennisplatform.membership.application.port.in.FeeView;
import com.tennisplatform.membership.application.port.in.ManageFees;
import com.tennisplatform.membership.application.port.in.QueryFees;
import com.tennisplatform.membership.application.port.out.FeeSettingsRepository;
import com.tennisplatform.membership.application.port.out.FixedGroupRepository;
import com.tennisplatform.membership.application.port.out.GroupSeriesRepository;
import com.tennisplatform.membership.application.port.out.MembershipRepository;
import com.tennisplatform.membership.application.port.out.MonthlyFeeRepository;
import com.tennisplatform.membership.domain.FeeCalendar;
import com.tennisplatform.membership.domain.FeeKind;
import com.tennisplatform.membership.domain.FeeNotFoundException;
import com.tennisplatform.membership.domain.FeePaymentMethod;
import com.tennisplatform.membership.domain.FeeSettings;
import com.tennisplatform.membership.domain.FixedGroup;
import com.tennisplatform.membership.domain.GroupMembership;
import com.tennisplatform.membership.domain.InvalidMembershipException;
import com.tennisplatform.membership.domain.MonthlyFee;
import com.tennisplatform.membership.domain.NotInGroupException;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** The fees the teacher records and how each month stands (43-fase28-analisis-mensualidades.md). */
public class FeesService implements ManageFees, QueryFees {

    private static final Logger log = LoggerFactory.getLogger(FeesService.class);

    private final MembershipRepository memberships;
    private final FixedGroupRepository groups;
    private final MonthlyFeeRepository fees;
    private final FeeSettingsRepository settings;
    private final FeeBook book;
    private final Clock clock;

    public FeesService(MembershipRepository memberships, FixedGroupRepository groups, GroupSeriesRepository groupSeries,
                       MonthlyFeeRepository fees, FeeSettingsRepository settings, GetLesson lessons,
                       GetTeacherProfile teacherProfile, Clock clock) {
        this.memberships = memberships;
        this.groups = groups;
        this.fees = fees;
        this.settings = settings;
        this.book = new FeeBook(groups, fees, settings, new GroupLessons(groupSeries, lessons), teacherProfile);
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeLineView> lines(UUID teacherUserId, YearMonth month, UUID studentUserId) {
        List<GroupMembership> found = memberships.findByTeacher(teacherUserId).stream()
                .filter(membership -> studentUserId == null || membership.studentUserId().equals(studentUserId))
                .toList();
        return book.lines(teacherUserId, found, month, clock.instant());
    }

    @Override
    @Transactional
    public FeeView record(UUID teacherUserId, NewFee fee) {
        GroupMembership membership = memberships.findById(fee.membershipId())
                .filter(found -> found.teacherUserId().equals(teacherUserId))
                .orElseThrow(() -> new NotInGroupException("No membership with id " + fee.membershipId()));
        requireMonthOf(membership, fee.month(), book.calendarOf(teacherUserId));

        MonthlyFee recorded;
        if (FeeKind.parse(fee.kind()) == FeeKind.WAIVED) {
            recorded = MonthlyFee.waived(membership.id(), fee.month(), clock.instant());
        } else {
            int amount = fee.amountCents() != null
                    ? fee.amountCents()
                    : groups.findById(membership.groupId()).map(FixedGroup::monthlyFeeCents).orElseThrow();
            recorded = MonthlyFee.paid(membership.id(), fee.month(), amount, fee.paidOn(),
                    FeePaymentMethod.parse(fee.paymentMethod()), clock.instant());
        }
        MonthlyFee saved = fees.save(recorded);
        log.info("Fee {} recorded for membership {} and {}", saved.id(), membership.id(), saved.month());
        return FeeView.from(saved);
    }

    @Override
    @Transactional
    public FeeView voidFee(UUID teacherUserId, UUID feeId) {
        MonthlyFee fee = fees.findById(feeId)
                .filter(found -> memberships.findById(found.membershipId())
                        .map(membership -> membership.teacherUserId().equals(teacherUserId))
                        .orElse(false))
                .orElseThrow(() -> new FeeNotFoundException("No fee with id " + feeId));
        MonthlyFee voided = fees.save(fee.voided(clock.instant()));
        log.info("Fee {} voided", feeId);
        return FeeView.from(voided);
    }

    @Override
    @Transactional(readOnly = true)
    public int dueDay(UUID teacherUserId) {
        return settings.of(teacherUserId).dueDay();
    }

    @Override
    @Transactional
    public int changeDueDay(UUID teacherUserId, int dueDay) {
        FeeSettings changed = new FeeSettings(dueDay);
        settings.save(teacherUserId, changed);
        log.info("Fee due day changed");
        return changed.dueDay();
    }

    @Override
    @Transactional(readOnly = true)
    public FeeIncome collectedBetween(UUID teacherUserId, LocalDate from, LocalDate until) {
        List<MonthlyFee> paid = fees.findPaidBetween(teacherUserId, from, until);
        return new FeeIncome(paid.size(), paid.stream().mapToLong(MonthlyFee::amountCents).sum());
    }

    /** A month from the one the student joined in to the last one their membership reaches. */
    private static void requireMonthOf(GroupMembership membership, YearMonth month, FeeCalendar calendar) {
        if (month == null) {
            throw new InvalidMembershipException("Say which month the fee is for");
        }
        YearMonth first = calendar.monthOf(membership.joinedAt());
        boolean afterTheEnd = membership.leftAt() != null
                && month.isAfter(calendar.monthOf(membership.leftAt().minusNanos(1)));
        if (month.isBefore(first) || afterTheEnd) {
            throw new InvalidMembershipException("The student was not in the group in " + month);
        }
    }
}
