package com.tennisplatform.notification.configuration;

import com.tennisplatform.identity.application.port.in.FindUserAccounts;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.notification.application.port.in.NotifyBookings;
import com.tennisplatform.notification.application.port.out.Mailer;
import com.tennisplatform.notification.application.service.BookingNotifications;
import com.tennisplatform.notification.application.service.MailTexts;
import com.tennisplatform.student.application.port.in.GetStudentProfile;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
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
}
