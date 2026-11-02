package com.tennisplatform.notification.configuration;

import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.identity.application.port.in.FindUserAccounts;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.membership.application.port.in.ManageGroups;
import com.tennisplatform.notification.application.port.in.NotifyBookings;
import com.tennisplatform.notification.application.port.in.NotifyCourtChanges;
import com.tennisplatform.notification.application.port.in.NotifyMemberships;
import com.tennisplatform.notification.application.port.in.NotifyMessages;
import com.tennisplatform.notification.application.port.out.Mailer;
import com.tennisplatform.notification.application.service.BookingNotifications;
import com.tennisplatform.notification.application.port.out.MessageRepository;
import com.tennisplatform.notification.application.service.CourtChangeNotifications;
import com.tennisplatform.notification.application.service.MailTexts;
import com.tennisplatform.notification.application.service.MessageNotifications;
import com.tennisplatform.notification.application.service.MembershipNotifications;
import com.tennisplatform.notification.application.service.MessagesService;
import com.tennisplatform.student.application.port.in.GetStudentProfile;
import com.tennisplatform.student.application.port.in.QueryManagedStudent;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.QueryLevels;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfiguration {

    @Bean
    public MailTexts mailTexts(NotificationProperties properties, Clock clock) {
        return new MailTexts(properties.getSchoolName(), properties.getMailFrom(), properties.getFrontendBaseUrl(),
                clock);
    }

    @Bean
    public NotifyBookings notifyBookings(Mailer mailer, MailTexts texts, FindUserAccounts accounts,
                                         GetStudentProfile students, GetTeacherProfile teachers, GetLesson lessons) {
        return new BookingNotifications(mailer, texts, accounts, students, teachers, lessons);
    }

    /**
     * Declared by its concrete type: one bean is the teacher's messages, each student's board and
     * the part of deleting an account that {@code student} asks this module for.
     */
    @Bean
    public MessagesService messages(MessageRepository messages, QueryManagedStudent students, QueryLevels levels,
                                    GetLesson lessons, GetBookings bookings, GetTeacherProfile teachers,
                                    ApplicationEventPublisher events, Clock clock) {
        return new MessagesService(messages, students, levels, lessons, bookings, teachers, events, clock);
    }

    @Bean
    public NotifyCourtChanges notifyCourtChanges(Mailer mailer, MailTexts texts, GetBookings bookings,
                                                 FindUserAccounts accounts, GetStudentProfile students,
                                                 GetTeacherProfile teachers) {
        return new CourtChangeNotifications(mailer, texts, bookings, accounts, students, teachers);
    }

    @Bean
    public NotifyMessages notifyMessages(Mailer mailer, MailTexts texts, MessageRepository messages,
                                         FindUserAccounts accounts, GetStudentProfile students,
                                         GetTeacherProfile teachers) {
        return new MessageNotifications(mailer, texts, messages, accounts, students, teachers);
    }

    @Bean
    public NotifyMemberships notifyMemberships(Mailer mailer, MailTexts texts, FindUserAccounts accounts,
                                               GetStudentProfile students, GetTeacherProfile teachers,
                                               ManageGroups groups) {
        return new MembershipNotifications(mailer, texts, accounts, students, teachers, groups);
    }
}
