package com.tennisplatform.notification;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.booking.application.port.in.BookingConfirmed;
import com.tennisplatform.notification.application.port.out.Mail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who hears about a booking, one test per row of the table in
 * 32-fase20-analisis-avisos-calendario.md (decision 2).
 */
class BookingNotificationsTest extends AbstractBookingTest {

    @Autowired
    private RecordingMailer mailer;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private TransactionTemplate transactions;

    private Student student;
    private UUID lessonId;

    @BeforeEach
    void aStudentAndALesson() {
        student = aStudentWhoMayBook();
        lessonId = anIndividualLessonIn(Duration.ofDays(3));
        mailer.clear();
    }

    @Test
    void aBookingSendsTheStudentTheLessonForTheirCalendarAndTellsTheTeacher() {
        UUID bookingId = bookingIdOf(book(student.token(), lessonId));

        Mail toStudent = only(student.email());
        assertThat(toStudent.kind()).isEqualTo("booking-confirmed-student");
        assertThat(toStudent.body()).startsWith("Hola, Lucia Prieto:");
        assertThat(toStudent.calendar().method()).isEqualTo("REQUEST");
        assertThat(toStudent.calendar().content()).contains("UID:" + bookingId + "@tennis-platform");

        Mail toTeacher = only(TEACHER_EMAIL);
        assertThat(toTeacher.kind()).isEqualTo("booking-confirmed-teacher");
        assertThat(toTeacher.subject()).startsWith("Nueva reserva: Lucia Prieto, ");
        assertThat(toTeacher.body()).contains("Plazas ocupadas: 1 de 1.");
        assertThat(mailer.sent()).hasSize(2);
    }

    @Test
    void aStudentWhoCancelsIsNotWrittenToButTheTeacherIs() {
        UUID bookingId = bookingIdOf(book(student.token(), lessonId));
        mailer.clear();

        assertThat(cancelBooking(student.token(), bookingId).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(mailer.to(student.email())).isEmpty();
        assertThat(only(TEACHER_EMAIL).kind()).isEqualTo("booking-cancelled-teacher");
    }

    @Test
    void theTeacherCancellingTheBookingRemovesTheLessonFromTheStudentsCalendar() {
        UUID bookingId = bookingIdOf(book(student.token(), lessonId));
        String invitation = only(student.email()).calendar().content();
        mailer.clear();

        assertThat(cancelBooking(teacherToken, bookingId).getStatusCode()).isEqualTo(HttpStatus.OK);

        Mail toStudent = only(student.email());
        assertThat(toStudent.kind()).isEqualTo("booking-cancelled-student");
        assertThat(toStudent.body()).contains("Ana Serrano ha cancelado tu clase");
        assertThat(toStudent.calendar().method()).isEqualTo("CANCEL");
        assertThat(uidOf(toStudent.calendar().content())).isEqualTo(uidOf(invitation));
        assertThat(mailer.to(TEACHER_EMAIL)).isEmpty();
    }

    @Test
    void theAdministrationCancellingTheLessonWritesToTheStudentOnly() {
        book(student.token(), lessonId);
        Student admin = aStudentWithAProfile();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", admin.id());
        mailer.clear();

        ResponseEntity<Map> cancelled = rest.exchange("/api/v1/admin/lessons/" + lessonId + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(tokenOf(admin.email(), PASSWORD))), Map.class);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(only(student.email()).body()).contains("La administración de la escuela ha cancelado tu clase");
        assertThat(mailer.sent()).hasSize(1);
    }

    @Test
    void aStudentTheTeacherLetsGoHearsOfEachCancelledBooking() {
        book(student.token(), lessonId);
        book(student.token(), anIndividualLessonIn(Duration.ofDays(4)));
        mailer.clear();

        ResponseEntity<Map> released = rest.exchange("/api/v1/teacher/students/" + student.id() + "/manage",
                HttpMethod.DELETE, new HttpEntity<>(bearer(teacherToken)), Map.class);

        assertThat(released.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(mailer.to(student.email())).hasSize(2)
                .allSatisfy(mail -> assertThat(mail.kind()).isEqualTo("booking-cancelled-student"));
        assertThat(mailer.to(TEACHER_EMAIL)).isEmpty();
    }

    @Test
    void aStudentWhoDeletesTheirAccountTriggersNoEmail() {
        book(student.token(), lessonId);
        mailer.clear();

        ResponseEntity<String> deleted = rest.exchange("/api/v1/me/deletion", HttpMethod.POST,
                new HttpEntity<>(Map.of("password", PASSWORD), jsonBearer(student.token())), String.class);

        assertThat(deleted.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(mailer.sent()).isEmpty();
    }

    /**
     * A refused booking never announces anything (BookLessonServiceTest), so the rollback is
     * exercised directly: a fact published in a transaction that then rolls back reaches nobody.
     */
    @Test
    void nothingIsSentForATransactionThatRolledBack() {
        BookingConfirmed confirmed = new BookingConfirmed(UUID.randomUUID(), lessonId, teacherId, student.id(),
                clock.instant().plus(Duration.ofDays(3)), clock.instant().plus(Duration.ofDays(3).plusHours(1)));

        transactions.executeWithoutResult(status -> {
            publisher.publishEvent(confirmed);
            status.setRollbackOnly();
        });
        assertThat(mailer.sent()).isEmpty();

        transactions.executeWithoutResult(status -> publisher.publishEvent(confirmed));
        assertThat(mailer.sent()).hasSize(2);
    }

    private Mail only(String address) {
        assertThat(mailer.to(address)).hasSize(1);
        return mailer.to(address).get(0);
    }

    private static String uidOf(String ics) {
        return ics.lines().filter(line -> line.startsWith("UID:")).findFirst().orElseThrow();
    }

    @SuppressWarnings("rawtypes")
    private static UUID bookingIdOf(ResponseEntity<Map> booked) {
        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) booked.getBody().get("id"));
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> cancelBooking(String token, UUID bookingId) {
        return rest.exchange("/api/v1/bookings/" + bookingId + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(token)), Map.class);
    }
}
