package com.tennisplatform.membership.configuration;

import com.tennisplatform.booking.application.port.in.GroupBookings;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.membership.application.port.out.FeeSettingsRepository;
import com.tennisplatform.membership.application.port.out.FixedGroupRepository;
import com.tennisplatform.membership.application.port.out.GroupSeriesRepository;
import com.tennisplatform.membership.application.port.out.MembershipRepository;
import com.tennisplatform.membership.application.port.out.MonthlyFeeRepository;
import com.tennisplatform.membership.application.service.FeesService;
import com.tennisplatform.membership.application.service.GroupsService;
import com.tennisplatform.membership.application.service.MembershipSeats;
import com.tennisplatform.membership.application.service.MembershipsService;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the use cases by hand, like the other modules do. {@code MembershipSeats} is this module's
 * answer to {@code booking}'s {@code GroupSeats}, and {@code MembershipsService} to
 * {@code student}'s {@code StudentMemberships}; both declared by concrete type.
 */
@Configuration
public class MembershipConfiguration {

    @Bean
    public GroupsService groupsService(FixedGroupRepository groups, GroupSeriesRepository groupSeries,
                                       MembershipRepository memberships, GetLesson lessons, GroupBookings seats,
                                       GetTeacherProfile teacherProfile, Clock clock) {
        return new GroupsService(groups, groupSeries, memberships, lessons, seats, teacherProfile, clock);
    }

    @Bean
    public MembershipsService membershipsService(FixedGroupRepository groups, GroupSeriesRepository groupSeries,
                                                 MembershipRepository memberships, MonthlyFeeRepository fees,
                                                 FeeSettingsRepository settings, GetLesson lessons,
                                                 GroupBookings seats, GetTeacherProfile teacherProfile,
                                                 ApplicationEventPublisher publisher, Clock clock) {
        return new MembershipsService(groups, groupSeries, memberships, fees, settings, lessons, seats,
                teacherProfile, publisher, clock);
    }

    @Bean
    public FeesService feesService(MembershipRepository memberships, FixedGroupRepository groups,
                                   GroupSeriesRepository groupSeries, MonthlyFeeRepository fees,
                                   FeeSettingsRepository settings, GetLesson lessons,
                                   GetTeacherProfile teacherProfile, Clock clock) {
        return new FeesService(memberships, groups, groupSeries, fees, settings, lessons, teacherProfile, clock);
    }

    @Bean
    public MembershipSeats membershipSeats(GroupSeriesRepository groupSeries, MembershipRepository memberships) {
        return new MembershipSeats(groupSeries, memberships);
    }
}
