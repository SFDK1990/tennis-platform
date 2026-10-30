package com.tennisplatform.pass;

import com.tennisplatform.lesson.domain.LessonType;
import com.tennisplatform.notification.RecordingMailer;
import com.tennisplatform.notification.application.port.out.Mail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The balance on every path of a seat (41-fase27-analisis-bonos.md, decisions 4 and 5), with the
 * teacher requiring passes. One test per path, as {@code WaitingListApiTest} does for the list.
 */
class PassBookingApiTest extends AbstractPassTest {

    private static final Duration IN_A_WEEK = Duration.ofDays(7);

    @Autowired
    private RecordingMailer mailer;

    private UUID groupPasses;
    private Student student;

    @BeforeEach
    void aStudentWithAPassOfThreeGroupLessons() {
        groupPasses = aPassType("3 de grupo", "GROUP", 3, null);
        student = aStudentWhoMayBook();
        requirePasses(true);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void withTheSwitchOffBookingNeedsNoPassAndSpendsNone() {
        requirePasses(false);
        UUID pass = aPassOf(student, groupPasses);

        ResponseEntity<Map> booked = book(student.token(), aGroupLessonIn(IN_A_WEEK));

        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(booked.getBody()).containsEntry("passId", null);
        assertThat(remainingOn(student, pass)).isEqualTo(3);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void withoutALessonLeftThereIsNoBooking() {
        ResponseEntity<Map> refused = book(student.token(), aGroupLessonIn(IN_A_WEEK));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(refused.getBody()).containsEntry("code", "NO_PASS_BALANCE");
    }

    /** Decision 1: a pass of group lessons does not pay for a private one. */
    @Test
    @SuppressWarnings("rawtypes")
    void aPassPaysOnlyForItsKindOfLesson() {
        aPassOf(student, groupPasses);

        ResponseEntity<Map> refused = book(student.token(), anIndividualLessonIn(IN_A_WEEK));

        assertThat(refused.getBody()).containsEntry("code", "NO_PASS_BALANCE");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void bookingSpendsALessonOfThePassUntilNoneIsLeft() {
        UUID pass = aPassOf(student, groupPasses);

        ResponseEntity<Map> booked = book(student.token(), aGroupLessonIn(IN_A_WEEK));

        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(booked.getBody()).containsEntry("passId", pass.toString());
        assertThat(passOf(student, pass)).containsEntry("used", 1).containsEntry("remaining", 2);
        book(student.token(), aGroupLessonIn(IN_A_WEEK.plusDays(1)));
        book(student.token(), aGroupLessonIn(IN_A_WEEK.plusDays(2)));
        assertThat(passOf(student, pass)).containsEntry("remaining", 0).containsEntry("status", "USED_UP");
        assertThat(book(student.token(), aGroupLessonIn(IN_A_WEEK.plusDays(3))).getBody())
                .containsEntry("code", "NO_PASS_BALANCE");
    }

    /** Decision 4: the pass that expires first pays, and one that never expires pays last. */
    @Test
    @SuppressWarnings("rawtypes")
    void theLessonComesOutOfThePassThatExpiresFirst() {
        UUID forever = aPassOf(student, groupPasses);
        UUID inSixMonths = aPassOf(student, aPassType("6 meses", "GROUP", 3, 6));
        UUID inTwoMonths = aPassOf(student, aPassType("2 meses", "GROUP", 1, 2));

        assertThat(book(student.token(), aGroupLessonIn(IN_A_WEEK)).getBody()).containsEntry("passId", inTwoMonths.toString());
        assertThat(book(student.token(), aGroupLessonIn(IN_A_WEEK.plusDays(1))).getBody())
                .containsEntry("passId", inSixMonths.toString());
        assertThat(remainingOn(student, forever)).isEqualTo(3);
    }

    /** Decision 4: an expired pass pays for nothing, and a valid one only for lessons up to its last day. */
    @Test
    @SuppressWarnings("rawtypes")
    void anExpiredPassPaysForNothingAndAValidOneOnlyUpToItsLastDay() {
        UUID oneMonth = aPassType("1 mes", "GROUP", 5, 1);
        UUID expired = aPassOf(student, oneMonth, today().minusMonths(2));
        assertThat(passOf(student, expired)).containsEntry("status", "EXPIRED");
        assertThat(book(student.token(), aGroupLessonIn(Duration.ofDays(2))).getBody())
                .containsEntry("code", "NO_PASS_BALANCE");

        // Valid until ten days from now: it pays for a lesson in five days, not for one in twenty.
        LocalDate paidOn = today().plusDays(10).minusMonths(1);
        UUID valid = aPassOf(student, oneMonth, paidOn);
        assertThat(passOf(student, valid)).containsEntry("validUntil", paidOn.plusMonths(1).toString());
        assertThat(book(student.token(), aGroupLessonIn(Duration.ofDays(20))).getBody())
                .containsEntry("code", "NO_PASS_BALANCE");
        assertThat(book(student.token(), aGroupLessonIn(Duration.ofDays(5))).getBody())
                .containsEntry("passId", valid.toString());
    }

    @Test
    void theStudentCancelsInTimeAndGetsTheLessonBack() {
        UUID pass = aPassOf(student, groupPasses);
        Object booking = book(student.token(), aGroupLessonIn(IN_A_WEEK)).getBody().get("id");

        assertThat(cancel(student.token(), booking).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(remainingOn(student, pass)).isEqualTo(3);
    }

    /** Inside the 24 hours the student cannot cancel, so the lesson stays spent. */
    @Test
    void theStudentCannotCancelInsideThe24HoursAndTheLessonStaysSpent() {
        UUID pass = aPassOf(student, groupPasses);
        Object booking = book(student.token(), aGroupLessonIn(Duration.ofHours(5))).getBody().get("id");

        assertThat(cancel(student.token(), booking).getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);

        assertThat(remainingOn(student, pass)).isEqualTo(2);
    }

    /** Decision 5: whatever the teacher cancels gives the lesson back, even inside the 24 hours. */
    @Test
    void theTeacherCancelsTheBookingInsideThe24HoursAndTheLessonComesBack() {
        UUID pass = aPassOf(student, groupPasses);
        Object booking = book(student.token(), aGroupLessonIn(Duration.ofHours(5))).getBody().get("id");

        assertThat(cancel(teacherToken, booking).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(remainingOn(student, pass)).isEqualTo(3);
    }

    @Test
    void anAdministratorCancelsTheBookingAndTheLessonComesBack() {
        UUID pass = aPassOf(student, groupPasses);
        Object booking = book(student.token(), aGroupLessonIn(Duration.ofHours(5))).getBody().get("id");

        assertThat(cancel(anAdminToken(), booking).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(remainingOn(student, pass)).isEqualTo(3);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherCancelsTheLessonAndTheLessonComesBack() {
        UUID pass = aPassOf(student, groupPasses);
        UUID lesson = aGroupLessonIn(Duration.ofHours(5));
        book(student.token(), lesson);

        ResponseEntity<Map> cancelled = rest.exchange("/api/v1/teacher/lessons/" + lesson + "/cancel",
                HttpMethod.POST, new HttpEntity<>(bearer(teacherToken)), Map.class);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(remainingOn(student, pass)).isEqualTo(3);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void anAdministratorCancelsTheLessonAndTheLessonComesBack() {
        UUID pass = aPassOf(student, groupPasses);
        UUID lesson = aGroupLessonIn(Duration.ofHours(5));
        book(student.token(), lesson);

        ResponseEntity<Map> cancelled = rest.exchange("/api/v1/admin/lessons/" + lesson + "/cancel",
                HttpMethod.POST, new HttpEntity<>(bearer(anAdminToken())), Map.class);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(remainingOn(student, pass)).isEqualTo(3);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aRainyDayGivesTheLessonBack() {
        UUID pass = aPassOf(student, groupPasses);
        LocalDate rainyDay = today().plusDays(9);
        UUID lesson = aLessonAt(rainyDay.atTime(LocalTime.of(18, 0)).atZone(MADRID).toInstant(), LessonType.GROUP, 4);
        book(student.token(), lesson);

        ResponseEntity<Map> cancelled = rest.exchange("/api/v1/teacher/lessons/cancellations", HttpMethod.POST,
                new HttpEntity<>(Map.of("date", rainyDay.toString()), jsonBearer(teacherToken)), Map.class);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(remainingOn(student, pass)).isEqualTo(3);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void disablingTheAccountGivesTheUpcomingLessonsBack() {
        UUID pass = aPassOf(student, groupPasses);
        book(student.token(), aGroupLessonIn(IN_A_WEEK));
        book(student.token(), aGroupLessonIn(IN_A_WEEK.plusDays(1)));

        ResponseEntity<Map> disabled = rest.exchange("/api/v1/admin/users/" + student.id() + "/status",
                HttpMethod.PATCH, new HttpEntity<>(Map.of("status", "DISABLED"), jsonBearer(anAdminToken())), Map.class);

        assertThat(disabled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(usedOf(pass)).isZero();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void deletingTheAccountGivesTheUpcomingLessonsBack() {
        UUID pass = aPassOf(student, groupPasses);
        book(student.token(), aGroupLessonIn(IN_A_WEEK));

        ResponseEntity<Map> deleted = rest.exchange("/api/v1/me/deletion", HttpMethod.POST,
                new HttpEntity<>(Map.of("password", PASSWORD), jsonBearer(student.token())), Map.class);

        assertThat(deleted.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(usedOf(pass)).isZero();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherLettingTheStudentGoGivesTheUpcomingLessonsBack() {
        UUID pass = aPassOf(student, groupPasses);
        book(student.token(), aGroupLessonIn(IN_A_WEEK));

        ResponseEntity<Map> released = rest.exchange("/api/v1/teacher/students/" + student.id() + "/manage",
                HttpMethod.DELETE, new HttpEntity<>(bearer(teacherToken)), Map.class);

        assertThat(released.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(usedOf(pass)).isZero();
    }

    /** Decision 5: the lesson goes back to the pass it came from, even if that one has expired since. */
    @Test
    void aCancelledLessonGoesBackToItsOwnPassEvenIfItHasExpired() {
        UUID first = aPassOf(student, aPassType("1 mes", "GROUP", 1, 1));
        UUID later = aPassOf(student, groupPasses);
        Object booking = book(student.token(), aGroupLessonIn(Duration.ofDays(3))).getBody().get("id");
        jdbc.update("UPDATE passes SET valid_until = ? WHERE id = ?", today().minusDays(1), first);

        assertThat(cancel(teacherToken, booking).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(passOf(student, first)).containsEntry("remaining", 1).containsEntry("status", "EXPIRED");
        assertThat(remainingOn(student, later)).isEqualTo(3);
    }

    /** Decision 5: a student who did not come spent the lesson when they booked it. */
    @Test
    @SuppressWarnings("rawtypes")
    void aStudentWhoDidNotComeKeepsTheLessonSpent() {
        UUID pass = aPassOf(student, groupPasses);
        UUID lesson = aGroupLessonIn(Duration.ofHours(5));
        Object booking = book(student.token(), lesson).getBody().get("id");
        jdbc.update("UPDATE lessons SET starts_at = starts_at - interval '6 hours', ends_at = ends_at - interval '6 hours' "
                + "WHERE id = ?", lesson);
        jdbc.update("UPDATE bookings SET lesson_starts_at = lesson_starts_at - interval '6 hours', "
                + "lesson_ends_at = lesson_ends_at - interval '6 hours' WHERE id = ?", UUID.fromString((String) booking));

        ResponseEntity<String> marked = rest.exchange("/api/v1/teacher/lessons/" + lesson + "/attendance",
                HttpMethod.POST, new HttpEntity<>(Map.of("entries", List.of(Map.of("bookingId", booking,
                        "status", "NO_SHOW"))), jsonBearer(teacherToken)), String.class);

        assertThat(marked.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(remainingOn(student, pass)).isEqualTo(2);
    }

    /** Decision 5: joining asks for a lesson left, without spending it. */
    @Test
    @SuppressWarnings("rawtypes")
    void joiningTheWaitingListAsksForALessonLeftWithoutSpendingIt() {
        UUID full = aFullLesson();

        ResponseEntity<Map> refused = join(student.token(), full);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(refused.getBody()).containsEntry("code", "NO_PASS_BALANCE");

        UUID pass = aPassOf(student, groupPasses);
        assertThat(join(student.token(), full).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(remainingOn(student, pass)).isEqualTo(3);
    }

    /** The seat handed over by the waiting list is paid with the student's pass. */
    @Test
    void theSeatFromTheWaitingListSpendsALessonOfThePass() {
        UUID full = aFullLesson();
        UUID pass = aPassOf(student, groupPasses);
        join(student.token(), full);

        assertThat(cancel(teacherToken, seatIn(full)).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(remainingOn(student, pass)).isEqualTo(2);
    }

    /** Decision 5: whoever has no lesson left when their turn comes is skipped, told, and off the list. */
    @Test
    void aWaitingStudentWithNoLessonLeftIsSkippedAndTold() {
        UUID full = aFullLesson();
        UUID pass = aPassOf(student, aPassType("Una", "GROUP", 1, null));
        join(student.token(), full);
        Student next = aStudentWhoMayBook();
        aPassOf(next, groupPasses);
        join(next.token(), full);
        book(student.token(), aGroupLessonIn(IN_A_WEEK.plusDays(3)));
        assertThat(remainingOn(student, pass)).isZero();
        mailer.clear();

        assertThat(cancel(teacherToken, seatIn(full)).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(seatOf(next, full)).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM waiting_list_entries WHERE lesson_id = ?",
                Integer.class, full)).isZero();
        assertThat(mailer.to(student.email())).extracting(Mail::subject)
                .anySatisfy(subject -> assertThat(subject).startsWith("Sin bono para la plaza"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aPassIsVoidedOnlyWhileNoLessonWasBookedWithIt() {
        UUID unused = aPassOf(student, groupPasses);
        UUID used = aPassOf(student, aPassType("Otro", "GROUP", 2, 1));
        book(student.token(), aGroupLessonIn(IN_A_WEEK));

        ResponseEntity<Map> voided = voidPass(unused);
        assertThat(voided.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(voided.getBody()).containsEntry("status", "VOIDED");
        assertThat(voidPass(unused).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> refused = voidPass(used);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refused.getBody()).containsEntry("code", "PASS_ALREADY_USED");
        assertThat(voidPass(UUID.randomUUID()).getBody()).containsEntry("code", "PASS_NOT_FOUND");

        // A voided pass pays for nothing: the next lesson finds no other pass with a lesson left.
        book(student.token(), aGroupLessonIn(IN_A_WEEK.plusDays(1)));
        assertThat(book(student.token(), aGroupLessonIn(IN_A_WEEK.plusDays(2))).getBody())
                .containsEntry("code", "NO_PASS_BALANCE");
    }

    /** Decision 6: the summary of the month adds up the passes sold, voided ones left out. */
    @Test
    @SuppressWarnings("rawtypes")
    void theMonthlySummaryAddsUpThePassesSold() {
        aPassOf(student, groupPasses);
        aPassOf(aStudentWhoMayBook(), groupPasses);
        UUID mistake = aPassOf(student, groupPasses);
        voidPass(mistake);
        aPassOf(student, groupPasses, today().minusMonths(1));

        Map summary = rest.exchange("/api/v1/teacher/summary", HttpMethod.GET, new HttpEntity<>(bearer(teacherToken)),
                Map.class).getBody();

        assertThat((Map) summary.get("sales")).containsEntry("passesSold", 2).containsEntry("totalCents", 6_000);
        assertThat((Map) summary.get("previousSales")).containsEntry("passesSold", 1);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theExportOfTheStudentCarriesTheirPasses() {
        UUID pass = aPassOf(student, groupPasses);
        book(student.token(), aGroupLessonIn(IN_A_WEEK));

        Map export = rest.exchange("/api/v1/me/export", HttpMethod.GET, new HttpEntity<>(bearer(student.token())),
                Map.class).getBody();

        assertThat((List<Map>) export.get("passes")).singleElement()
                .satisfies(exported -> assertThat(exported).containsEntry("id", pass.toString()));
        assertThat((List<Map>) export.get("bookings")).singleElement()
                .satisfies(exported -> assertThat(exported).containsEntry("passId", pass.toString()));
    }

    /** A group lesson of one seat, taken by somebody with their own pass. */
    private UUID aFullLesson() {
        UUID lesson = aLessonStartingIn(Duration.ofDays(30), LessonType.GROUP, 1);
        Student holder = aStudentWhoMayBook();
        aPassOf(holder, groupPasses);
        assertThat(book(holder.token(), lesson).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return lesson;
    }

    private UUID aGroupLessonIn(Duration fromNow) {
        return aLessonStartingIn(fromNow, LessonType.GROUP, 4);
    }

    private UUID seatIn(UUID lesson) {
        return jdbc.queryForObject("SELECT id FROM bookings WHERE lesson_id = ? AND status = 'CONFIRMED'",
                UUID.class, lesson);
    }

    private UUID seatOf(Student who, UUID lesson) {
        return jdbc.queryForObject("SELECT id FROM bookings WHERE lesson_id = ? AND student_user_id = ? "
                + "AND status = 'CONFIRMED'", UUID.class, lesson, who.id());
    }

    private int usedOf(UUID pass) {
        Integer used = jdbc.queryForObject("SELECT count(*) FROM bookings WHERE pass_id = ? AND status = 'CONFIRMED'",
                Integer.class, pass);
        return used == null ? 0 : used;
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> voidPass(UUID pass) {
        return rest.exchange("/api/v1/teacher/passes/" + pass + "/void", HttpMethod.POST,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
    }
}
