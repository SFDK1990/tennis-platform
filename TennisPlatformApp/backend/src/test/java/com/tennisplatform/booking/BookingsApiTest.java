package com.tennisplatform.booking;

import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.booking.domain.Booking;
import com.tennisplatform.booking.domain.BookingAlreadyExistsException;
import com.tennisplatform.booking.domain.StudentScheduleOverlapException;
import com.tennisplatform.lesson.domain.LessonType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The booking endpoints over real HTTP, against a real PostgreSQL. The rules themselves are
 * covered by the domain and service tests; this level shows the schema, the constraints, the
 * cascades that cross modules, and who may call what. The race for the last seat has its own
 * class, {@code LastSeatConcurrencyTest}.
 */
class BookingsApiTest extends AbstractBookingTest {

    private static final Duration IN_A_MONTH = Duration.ofDays(30);

    @Autowired
    private BookingRepository bookings;

    @Test
    @SuppressWarnings("rawtypes")
    void aManagedStudentBooksASeatAndTheLessonCountsIt() {
        Student student = aStudentWhoMayBook();
        UUID lessonId = aLessonStartingIn(IN_A_MONTH, LessonType.GROUP, 4);

        ResponseEntity<Map> booked = book(student.token(), lessonId);

        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(booked.getBody()).containsEntry("status", "CONFIRMED")
                .containsEntry("attendance", "PENDING");
        assertThat(lessonOf(booked)).containsEntry("bookedCount", 1).containsEntry("status", "OPEN");
        assertThat(readLesson(student.token(), lessonId)).containsEntry("bookedCount", 1);
    }

    /** Criterion: the last seat turns the lesson FULL, and giving it back opens it again. */
    @Test
    @SuppressWarnings("rawtypes")
    void theLastSeatMakesTheLessonFullAndCancellingOpensItAgain() {
        Student student = aStudentWhoMayBook();
        UUID lessonId = anIndividualLessonIn(IN_A_MONTH);

        ResponseEntity<Map> booked = book(student.token(), lessonId);
        assertThat(lessonOf(booked)).containsEntry("status", "FULL");

        ResponseEntity<Map> refused = book(aStudentWhoMayBook().token(), lessonId);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refused.getBody()).containsEntry("code", "LESSON_FULL");

        ResponseEntity<Map> cancelled = cancel(student.token(), idOf(booked));
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody()).containsEntry("status", "CANCELLED_BY_STUDENT");
        assertThat(readLesson(student.token(), lessonId)).containsEntry("status", "OPEN")
                .containsEntry("bookedCount", 0);
    }

    /** Decision 4: a student who gave a seat back may take it again. */
    @Test
    @SuppressWarnings("rawtypes")
    void aStudentMayBookTheSameLessonAgainAfterCancelling() {
        Student student = aStudentWhoMayBook();
        UUID lessonId = anIndividualLessonIn(IN_A_MONTH);
        cancel(student.token(), idOf(book(student.token(), lessonId)));

        ResponseEntity<Map> again = book(student.token(), lessonId);

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void refusesASecondSeatInTheSameLesson() {
        Student student = aStudentWhoMayBook();
        UUID lessonId = aLessonStartingIn(IN_A_MONTH, LessonType.GROUP, 4);
        book(student.token(), lessonId);

        ResponseEntity<Map> second = book(student.token(), lessonId);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).containsEntry("code", "BOOKING_ALREADY_EXISTS");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aStudentTheTeacherDoesNotManageCannotBook() {
        Student student = aStudentWithAProfile();
        verify(student);
        String token = tokenOf(student.email(), PASSWORD);

        ResponseEntity<Map> refused = book(token, anIndividualLessonIn(IN_A_MONTH));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refused.getBody()).containsEntry("code", "STUDENT_NOT_MANAGED");
    }

    /**
     * Logging in, filling the profile in and being managed all work without a verified address,
     * so booking is the only place that rule can hold - and it has to.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void aStudentWithoutAVerifiedAddressCannotBook() {
        Student student = aStudentWithAProfile();
        manage(student);

        ResponseEntity<Map> refused = book(student.token(), anIndividualLessonIn(IN_A_MONTH));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refused.getBody()).containsEntry("code", "EMAIL_NOT_VERIFIED");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherCannotBookASeat() {
        ResponseEntity<Map> refused = book(teacherToken, anIndividualLessonIn(IN_A_MONTH));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refused.getBody()).containsEntry("code", "AUTH_FORBIDDEN");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void refusesALessonThatWasCancelledOrHasStarted() {
        Student student = aStudentWhoMayBook();
        UUID cancelledLesson = anIndividualLessonIn(IN_A_MONTH);
        cancelLesson(cancelledLesson);
        UUID startedLesson = anIndividualLessonIn(Duration.ofMinutes(-30));

        ResponseEntity<Map> cancelled = book(student.token(), cancelledLesson);
        ResponseEntity<Map> started = book(student.token(), startedLesson);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(cancelled.getBody()).containsEntry("code", "LESSON_NOT_BOOKABLE");
        assertThat(started.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(started.getBody()).containsEntry("code", "LESSON_ALREADY_STARTED");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void answersNotFoundForALessonThatDoesNotExist() {
        ResponseEntity<Map> missing = book(aStudentWhoMayBook().token(), UUID.randomUUID());

        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).containsEntry("code", "LESSON_NOT_FOUND");
    }

    /** The 24-hour window binds the student over their own booking, and nobody else. */
    @Test
    @SuppressWarnings("rawtypes")
    void theStudentCannotCancelInsideTheWindowButTheTeacherAndAnAdminCan() {
        Student student = aStudentWhoMayBook();
        UUID first = anIndividualLessonIn(Duration.ofHours(5));
        UUID second = anIndividualLessonIn(Duration.ofHours(7));
        String firstBooking = idOf(book(student.token(), first));
        String secondBooking = idOf(book(student.token(), second));

        ResponseEntity<Map> byStudent = cancel(student.token(), firstBooking);
        assertThat(byStudent.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(byStudent.getBody()).containsEntry("code", "CANCELLATION_WINDOW_EXPIRED");

        ResponseEntity<Map> byTeacher = cancel(teacherToken, firstBooking);
        assertThat(byTeacher.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(byTeacher.getBody()).containsEntry("status", "CANCELLED_BY_TEACHER");

        ResponseEntity<Map> byAdmin = cancel(anAdminToken(), secondBooking);
        assertThat(byAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(byAdmin.getBody()).containsEntry("status", "CANCELLED_BY_ADMIN");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void cancellingTheSameBookingTwiceIsAConflict() {
        Student student = aStudentWhoMayBook();
        String bookingId = idOf(book(student.token(), anIndividualLessonIn(IN_A_MONTH)));
        cancel(student.token(), bookingId);

        ResponseEntity<Map> again = cancel(student.token(), bookingId);

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(again.getBody()).containsEntry("code", "BOOKING_ALREADY_CANCELLED");
    }

    /** Somebody else's booking answers exactly like one that does not exist. */
    @Test
    @SuppressWarnings("rawtypes")
    void aStudentCannotCancelSomebodyElsesBooking() {
        String bookingId = idOf(book(aStudentWhoMayBook().token(), anIndividualLessonIn(IN_A_MONTH)));

        ResponseEntity<Map> refused = cancel(aStudentWhoMayBook().token(), bookingId);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(refused.getBody()).containsEntry("code", "BOOKING_NOT_FOUND");
    }

    /** Criterion: cancelling a lesson cancels its bookings, in the same transaction. */
    @Test
    @SuppressWarnings("rawtypes")
    void cancellingALessonCancelsItsBookings() {
        Student student = aStudentWhoMayBook();
        UUID lessonId = aLessonStartingIn(IN_A_MONTH, LessonType.GROUP, 4);
        String bookingId = idOf(book(student.token(), lessonId));

        ResponseEntity<Map> cancelled = cancelLesson(lessonId);

        assertThat(cancelled.getBody()).containsEntry("bookedCount", 0);
        assertThat(bookings.findById(UUID.fromString(bookingId)).orElseThrow().status().name())
                .isEqualTo("CANCELLED_BY_TEACHER");
    }

    /**
     * Criterion carried over from Fase 6: letting a student go cancels their upcoming bookings,
     * and leaves alone those of lessons that already started, which are history.
     */
    @Test
    void deactivatingAStudentCancelsTheirUpcomingBookingsAndKeepsThePastOnes() {
        Student student = aStudentWhoMayBook();
        String upcoming = idOf(book(student.token(), anIndividualLessonIn(IN_A_MONTH)));
        UUID pastLesson = anIndividualLessonIn(Duration.ofDays(-2));
        Booking past = aBookingIn(pastLesson, student.id());

        rest.exchange("/api/v1/teacher/students/" + student.id() + "/manage", HttpMethod.DELETE,
                new HttpEntity<>(bearer(teacherToken)), Void.class);

        assertThat(bookings.findById(UUID.fromString(upcoming)).orElseThrow().status().name())
                .isEqualTo("CANCELLED_BY_TEACHER");
        assertThat(bookings.findById(past.id()).orElseThrow().isConfirmed()).isTrue();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theTeacherRecordsAttendanceOnceTheLessonHasStartedAndMayCorrectIt() {
        Student student = aStudentWhoMayBook();
        UUID future = anIndividualLessonIn(IN_A_MONTH);
        String futureBooking = idOf(book(student.token(), future));
        ResponseEntity<Map> tooEarly = markAttendanceAsMap(future, futureBooking, "ATTENDED");
        assertThat(tooEarly.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(tooEarly.getBody()).containsEntry("code", "ATTENDANCE_NOT_YET_OPEN");

        UUID started = anIndividualLessonIn(Duration.ofMinutes(-30));
        Booking booking = aBookingIn(started, student.id());

        ResponseEntity<List> marked = markAttendance(started, booking.id().toString(), "ATTENDED");
        ResponseEntity<List> corrected = markAttendance(started, booking.id().toString(), "NO_SHOW");

        assertThat(marked.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) corrected.getBody().get(0)).containsEntry("attendance", "NO_SHOW");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aStudentCannotRecordAttendance() {
        Student student = aStudentWhoMayBook();
        UUID started = anIndividualLessonIn(Duration.ofMinutes(-30));
        Booking booking = aBookingIn(started, student.id());

        ResponseEntity<Map> refused = rest.exchange(attendancePath(started), HttpMethod.POST,
                new HttpEntity<>(attendanceBody(booking.id().toString(), "ATTENDED"),
                        jsonBearer(student.token())), Map.class);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refused.getBody()).containsEntry("code", "TEACHER_FORBIDDEN");
    }

    /** All or nothing, and each way an entry can be wrong has its own answer. */
    @Test
    @SuppressWarnings("rawtypes")
    void attendanceIsRefusedForAnUnknownStatusABookingOfAnotherLessonOrACancelledOne() {
        Student student = aStudentWhoMayBook();
        UUID started = anIndividualLessonIn(Duration.ofMinutes(-30));
        Booking booking = aBookingIn(started, student.id());

        ResponseEntity<Map> unknownStatus = markAttendanceAsMap(started, booking.id().toString(), "MAYBE");
        assertThat(unknownStatus.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map> notOfThisLesson = markAttendanceAsMap(started, UUID.randomUUID().toString(), "ATTENDED");
        assertThat(notOfThisLesson.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notOfThisLesson.getBody()).containsEntry("code", "BOOKING_NOT_FOUND");

        // Cancelled while it still could be, before the lesson started.
        bookings.save(booking.cancelByAdmin(clock.instant().minus(Duration.ofHours(1))));
        ResponseEntity<Map> cancelled = markAttendanceAsMap(started, booking.id().toString(), "ATTENDED");
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(cancelled.getBody()).containsEntry("code", "BOOKING_ALREADY_CANCELLED");
    }

    /** An admin cancels bookings but has no list of them: that is a student's or the teacher's. */
    @Test
    @SuppressWarnings("rawtypes")
    void anAdminHasNoListOfBookings() {
        ResponseEntity<Map> response = list(anAdminToken(), "");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("code", "AUTH_FORBIDDEN");
    }

    /** The attendance screen needs exactly the bookings of one lesson, which is what lessonId gives it. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void eachSideListsWhatIsTheirs() {
        Student ana = aStudentWhoMayBook();
        Student luis = aStudentWhoMayBook();
        UUID group = aLessonStartingIn(IN_A_MONTH, LessonType.GROUP, 4);
        UUID other = aLessonStartingIn(IN_A_MONTH.plusHours(2), LessonType.GROUP, 4);
        book(ana.token(), group);
        book(luis.token(), group);
        book(ana.token(), other);

        ResponseEntity<Map> anas = list(ana.token(), "");
        ResponseEntity<Map> ofTheGroup = list(teacherToken, "?lessonId=" + group);
        ResponseEntity<Map> badFilter = list(ana.token(), "?status=ATTENDED");

        assertThat(anas.getBody()).containsEntry("totalItems", 2);
        assertThat(ofTheGroup.getBody()).containsEntry("totalItems", 2);
        assertThat((List<Map<String, Object>>) ofTheGroup.getBody().get("items"))
                .allSatisfy(item -> assertThat(item).containsEntry("lessonId", group.toString()));
        assertThat(badFilter.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * Every overlap the API can produce is stopped by the application's own check first - and with
     * a single teacher whose lessons cannot overlap, the API cannot produce one at all. Going
     * straight to the repository leaves only the schema between the two bookings, which is the
     * arrangement two concurrent requests would find, and shows the violation is translated.
     */
    @Test
    void theDatabaseRefusesAnOverlappingBookingThatSkippedTheApplicationCheck() {
        Student student = aStudentWhoMayBook();
        UUID first = anIndividualLessonIn(IN_A_MONTH);
        UUID second = anIndividualLessonIn(IN_A_MONTH.plusHours(2));
        Instant startsAt = clock.instant().plus(IN_A_MONTH);
        bookings.save(Booking.confirm(first, teacherId, student.id(), startsAt,
                startsAt.plus(Duration.ofHours(1)), clock.instant()));

        assertThatThrownBy(() -> bookings.save(Booking.confirm(second, teacherId, student.id(),
                startsAt.plus(Duration.ofMinutes(30)), startsAt.plus(Duration.ofMinutes(90)),
                clock.instant())))
                .isInstanceOf(StudentScheduleOverlapException.class);
    }

    @Test
    void theDatabaseRefusesASecondConfirmedSeatThatSkippedTheApplicationCheck() {
        Student student = aStudentWhoMayBook();
        UUID lessonId = aLessonStartingIn(IN_A_MONTH, LessonType.GROUP, 4);
        aBookingIn(lessonId, student.id());

        assertThatThrownBy(() -> aBookingIn(lessonId, student.id()))
                .isInstanceOf(BookingAlreadyExistsException.class);
    }

    // --- helpers ------------------------------------------------------------------------------

    /** A booking written through the repository, for lessons the API would no longer let anyone book. */
    private Booking aBookingIn(UUID lessonId, UUID studentId) {
        Instant startsAt = jdbc.queryForObject("SELECT starts_at FROM lessons WHERE id = ?",
                java.sql.Timestamp.class, lessonId).toInstant();
        return bookings.save(Booking.confirm(lessonId, teacherId, studentId, startsAt,
                startsAt.plus(Duration.ofHours(1)), startsAt.minus(Duration.ofDays(1))));
    }

    /**
     * An admin cannot be created through any endpoint, by design. A registered account is
     * promoted in the table, and logs in again so its token carries the role.
     */
    private String anAdminToken() {
        Student account = aStudentWithAProfile();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", account.id());
        return tokenOf(account.email(), PASSWORD);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> cancel(String token, String bookingId) {
        return rest.exchange("/api/v1/bookings/" + bookingId + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(token)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> cancelLesson(UUID lessonId) {
        return rest.exchange("/api/v1/teacher/lessons/" + lessonId + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> list(String token, String query) {
        return rest.exchange("/api/v1/bookings" + query, HttpMethod.GET, new HttpEntity<>(bearer(token)),
                Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<List> markAttendance(UUID lessonId, String bookingId, String attendance) {
        return rest.exchange(attendancePath(lessonId), HttpMethod.POST,
                new HttpEntity<>(attendanceBody(bookingId, attendance), jsonBearer(teacherToken)),
                List.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> markAttendanceAsMap(UUID lessonId, String bookingId, String attendance) {
        return rest.exchange(attendancePath(lessonId), HttpMethod.POST,
                new HttpEntity<>(attendanceBody(bookingId, attendance), jsonBearer(teacherToken)),
                Map.class);
    }

    private static String attendancePath(UUID lessonId) {
        return "/api/v1/teacher/lessons/" + lessonId + "/attendance";
    }

    private static Map<String, Object> attendanceBody(String bookingId, String attendance) {
        return Map.of("entries", List.of(Map.of("bookingId", bookingId, "status", attendance)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Map<String, Object> readLesson(String token, UUID lessonId) {
        return rest.exchange("/api/v1/lessons/" + lessonId, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), Map.class).getBody();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, Object> lessonOf(ResponseEntity<Map> booking) {
        return (Map<String, Object>) booking.getBody().get("lesson");
    }

    @SuppressWarnings("rawtypes")
    private static String idOf(ResponseEntity<Map> response) {
        return (String) response.getBody().get("id");
    }
}
