package com.tennisplatform.notification.application.service;

import com.tennisplatform.booking.application.port.in.BookingView;
import com.tennisplatform.booking.application.port.in.GetBookings;
import com.tennisplatform.identity.application.port.in.FindUserAccounts;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.lesson.application.port.in.LessonCourtChanged;
import com.tennisplatform.notification.application.port.in.NotifyCourtChanges;
import com.tennisplatform.notification.application.port.out.Mailer;
import com.tennisplatform.student.application.port.in.GetStudentProfile;
import com.tennisplatform.student.application.port.in.StudentProfileView;
import com.tennisplatform.teacher.application.port.in.GetTeacherProfile;
import com.tennisplatform.teacher.application.port.in.TeacherProfileView;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Only the students who hold a seat now hear about the new court: one who cancelled is not
 * coming, and one on the waiting list will hear when they get a seat. A deleted account keeps a
 * made-up address that nobody reads.
 */
public class CourtChangeNotifications implements NotifyCourtChanges {

    private static final String DELETED = "DELETED";

    private final Mailer mailer;
    private final MailTexts texts;
    private final GetBookings bookings;
    private final FindUserAccounts accounts;
    private final GetStudentProfile students;
    private final GetTeacherProfile teachers;

    public CourtChangeNotifications(Mailer mailer, MailTexts texts, GetBookings bookings, FindUserAccounts accounts,
                                    GetStudentProfile students, GetTeacherProfile teachers) {
        this.mailer = mailer;
        this.texts = texts;
        this.bookings = bookings;
        this.accounts = accounts;
        this.students = students;
        this.teachers = teachers;
    }

    @Override
    public void changed(LessonCourtChanged change) {
        List<UUID> seated = bookings.confirmedInLessons(List.of(change.lessonId())).stream()
                .map(BookingView::studentUserId)
                .toList();
        if (seated.isEmpty()) {
            return;
        }
        TeacherProfileView teacher = teachers.byUserId(change.teacherUserId()).orElseThrow();
        MailTexts.Lesson lesson = new MailTexts.Lesson(null, change.lessonId(), change.type(), change.startsAt(),
                change.endsAt(), ZoneId.of(teacher.timezone()), teacher.displayName());
        Map<UUID, UserSummary> found = accounts.byIds(seated);
        for (UUID studentUserId : seated) {
            UserSummary account = found.get(studentUserId);
            if (account == null || DELETED.equals(account.status())) {
                continue;
            }
            String name = students.byUserId(studentUserId).map(StudentProfileView::fullName).orElse(null);
            mailer.send(texts.courtChangedToStudent(lesson, change.courtName(), account.email(), name));
        }
    }
}
