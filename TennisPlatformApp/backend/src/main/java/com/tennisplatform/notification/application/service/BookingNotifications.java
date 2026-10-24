package com.tennisplatform.notification.application.service;

import com.tennisplatform.booking.application.port.in.BookingCancelled;
import com.tennisplatform.booking.application.port.in.BookingConfirmed;
import com.tennisplatform.identity.application.port.in.FindUserAccounts;
import com.tennisplatform.identity.application.port.in.UserSummary;
import com.tennisplatform.lesson.application.port.in.GetLesson;
import com.tennisplatform.lesson.application.port.in.LessonView;
import com.tennisplatform.notification.application.port.in.NotifyBookings;
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
 * Works out who hears about a booking and asks {@link MailTexts} what to tell them.
 *
 * <p>The facts carry ids only; the names, addresses and the teacher's zone are read here, after
 * the commit, so what is written is what the database now says.
 */
public class BookingNotifications implements NotifyBookings {

    private static final String DELETED = "DELETED";
    private static final String BY_STUDENT = "CANCELLED_BY_STUDENT";
    private static final String BY_ADMIN = "CANCELLED_BY_ADMIN";

    private final Mailer mailer;
    private final MailTexts texts;
    private final FindUserAccounts accounts;
    private final GetStudentProfile students;
    private final GetTeacherProfile teachers;
    private final GetLesson lessons;

    public BookingNotifications(Mailer mailer, MailTexts texts, FindUserAccounts accounts,
                                GetStudentProfile students, GetTeacherProfile teachers, GetLesson lessons) {
        this.mailer = mailer;
        this.texts = texts;
        this.accounts = accounts;
        this.students = students;
        this.teachers = teachers;
        this.lessons = lessons;
    }

    @Override
    public void confirmed(BookingConfirmed booking) {
        People people = people(booking.teacherUserId(), booking.studentUserId());
        LessonView lesson = lessons.byId(booking.lessonId());
        MailTexts.Lesson details = details(booking.bookingId(), lesson, people.teacher());

        if (people.student() != null) {
            String to = people.student().email();
            mailer.send(booking.fromWaitingList()
                    ? texts.waitingListSeatToStudent(details, to, people.studentName())
                    : texts.confirmationToStudent(details, to, people.studentName()));
        }
        if (people.teacherAccount() != null) {
            mailer.send(texts.bookingToTeacher(details, people.teacherAccount().email(), people.studentLabel(),
                    lesson.bookedCount(), lesson.capacity(), booking.fromWaitingList()));
        }
    }

    @Override
    public void cancelled(BookingCancelled booking) {
        People people = people(booking.teacherUserId(), booking.studentUserId());
        LessonView lesson = lessons.byId(booking.lessonId());
        MailTexts.Lesson details = details(booking.bookingId(), lesson, people.teacher());

        if (BY_STUDENT.equals(booking.status())) {
            if (people.teacherAccount() != null) {
                mailer.send(texts.studentCancellationToTeacher(details, people.teacherAccount().email(),
                        people.studentLabel()));
            }
            return;
        }
        if (people.student() != null) {
            String byWhom = BY_ADMIN.equals(booking.status()) ? null : people.teacher().displayName();
            mailer.send(texts.cancellationToStudent(details, people.student().email(), people.studentName(),
                    byWhom, booking.reason()));
        }
    }

    private MailTexts.Lesson details(UUID bookingId, LessonView lesson, TeacherProfileView teacher) {
        return new MailTexts.Lesson(bookingId, lesson.id(), lesson.type(), lesson.startsAt(), lesson.endsAt(),
                ZoneId.of(teacher.timezone()), teacher.displayName());
    }

    private People people(UUID teacherUserId, UUID studentUserId) {
        Map<UUID, UserSummary> found = accounts.byIds(List.of(teacherUserId, studentUserId));
        UserSummary student = found.get(studentUserId);
        // A deleted account keeps a made-up address that nobody reads.
        if (student != null && DELETED.equals(student.status())) {
            student = null;
        }
        String name = students.byUserId(studentUserId).map(StudentProfileView::fullName).orElse(null);
        TeacherProfileView teacher = teachers.byUserId(teacherUserId).orElseThrow();
        return new People(teacher, found.get(teacherUserId), student, name);
    }

    private record People(TeacherProfileView teacher, UserSummary teacherAccount, UserSummary student,
                          String studentName) {

        /** How the teacher knows the student: by name, or by address until they set one. */
        String studentLabel() {
            if (studentName != null && !studentName.isBlank()) {
                return studentName;
            }
            return student != null ? student.email() : "Un alumno";
        }
    }
}
