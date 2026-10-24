package com.tennisplatform.booking;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The waiting list of 34-fase21-analisis-lista-espera.md over real HTTP. One test per way a seat
 * frees up, each showing that the first student waiting gets it and nobody else does.
 */
class WaitingListApiTest extends AbstractBookingTest {

    private static final Duration IN_A_MONTH = Duration.ofDays(30);

    @Autowired
    private RecordingMailer mailer;

    private UUID lessonId;
    private Student holder;
    private Student first;
    private Student second;

    /** A lesson of one seat, taken, with two students waiting for it in order. */
    @BeforeEach
    void aFullLessonWithTwoStudentsWaiting() {
        lessonId = anIndividualLessonIn(IN_A_MONTH);
        holder = aStudentWhoMayBook();
        first = aStudentWhoMayBook();
        second = aStudentWhoMayBook();
        assertThat(book(holder.token(), lessonId).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(join(first.token(), lessonId).getBody()).containsEntry("position", 1);
        assertThat(join(second.token(), lessonId).getBody()).containsEntry("position", 2);
        mailer.clear();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theStudentSeesTheirPlaceAndTheTeacherSeesWhoWaitsInOrder() {
        List<Map> mine = (List<Map>) get(second.token(), "/api/v1/waiting-list").getBody().get("items");
        assertThat(mine).singleElement().satisfies(place -> {
            assertThat(place).containsEntry("position", 2);
            assertThat((Map) place.get("lesson")).containsEntry("id", lessonId.toString())
                    .containsEntry("status", "FULL");
        });

        List<Map> waiting = (List<Map>) get(teacherToken, "/api/v1/teacher/lessons/" + lessonId + "/waiting-list")
                .getBody().get("items");
        assertThat(waiting).extracting(entry -> entry.get("studentUserId"))
                .containsExactly(first.id().toString(), second.id().toString());
    }

    @Test
    @SuppressWarnings("rawtypes")
    void refusesWhatBookingWouldRefuseAndALessonWithAFreeSeat() {
        assertThat(join(first.token(), lessonId).getBody()).containsEntry("code", "ALREADY_WAITING");
        assertThat(join(holder.token(), lessonId).getBody()).containsEntry("code", "BOOKING_ALREADY_EXISTS");
        assertThat(join(teacherToken, lessonId).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        UUID withRoom = aLessonStartingIn(IN_A_MONTH.plusDays(1), LessonType.GROUP, 4);
        ResponseEntity<Map> notFull = join(first.token(), withRoom);
        assertThat(notFull.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(notFull.getBody()).containsEntry("code", "LESSON_NOT_FULL");

        Student unverified = aStudentWithAProfile();
        manage(unverified);
        assertThat(join(unverified.token(), lessonId).getBody()).containsEntry("code", "EMAIL_NOT_VERIFIED");

        Student stranger = aStudentWithAProfile();
        verify(stranger);
        assertThat(join(tokenOf(stranger.email(), PASSWORD), lessonId).getBody())
                .containsEntry("code", "STUDENT_NOT_MANAGED");

        UUID started = aLessonStartingIn(Duration.ofMinutes(-30), LessonType.INDIVIDUAL, 1);
        assertThat(join(first.token(), started).getBody()).containsEntry("code", "LESSON_ALREADY_STARTED");

        UUID cancelled = anIndividualLessonIn(IN_A_MONTH.plusDays(3));
        rest.exchange("/api/v1/teacher/lessons/" + cancelled + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
        assertThat(join(first.token(), cancelled).getBody()).containsEntry("code", "LESSON_NOT_BOOKABLE");
    }

    /** A list past its closing point is no longer shown to the student, and a lesson nobody waits for has none. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void aClosedListIsNoLongerShownAndAnEmptyOneIsEmpty() {
        UUID tomorrow = anIndividualLessonIn(Duration.ofHours(20));
        jdbc.update("INSERT INTO waiting_list_entries (lesson_id, teacher_user_id, student_user_id, joined_at) "
                + "VALUES (?, ?, ?, now())", tomorrow, teacherId, first.id());

        List<Map> mine = (List<Map>) get(first.token(), "/api/v1/waiting-list").getBody().get("items");
        assertThat(mine).extracting(place -> ((Map) place.get("lesson")).get("id")).containsExactly(lessonId.toString());

        List<Map> nobody = (List<Map>) get(teacherToken, "/api/v1/teacher/lessons/" + aLessonStartingIn(
                IN_A_MONTH.plusDays(4), LessonType.GROUP, 4) + "/waiting-list").getBody().get("items");
        assertThat(nobody).isEmpty();
        assertThat((List<Map>) get(holder.token(), "/api/v1/waiting-list").getBody().get("items")).isEmpty();
    }

    /** Decision 4: the list closes 24 hours before the lesson. */
    @Test
    @SuppressWarnings("rawtypes")
    void theListIsClosedLessThan24HoursBeforeTheLesson() {
        UUID tomorrow = anIndividualLessonIn(Duration.ofHours(20));
        assertThat(book(holder.token(), tomorrow).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> closed = join(first.token(), tomorrow);

        assertThat(closed.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(closed.getBody()).containsEntry("code", "WAITING_LIST_CLOSED");
    }

    /** Decision 4: a seat freed inside the last 24 hours stays free for whoever books it. */
    @Test
    void aSeatFreedLessThan24HoursBeforeIsNotHandedToTheList() {
        UUID tomorrow = anIndividualLessonIn(Duration.ofHours(20));
        UUID booking = bookingOf(book(holder.token(), tomorrow));
        jdbc.update("INSERT INTO waiting_list_entries (lesson_id, teacher_user_id, student_user_id, joined_at) "
                + "VALUES (?, ?, ?, now())", tomorrow, teacherId, first.id());

        assertThat(cancel(teacherToken, booking).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(confirmedBookingsOf(tomorrow)).isZero();
    }

    @Test
    void theStudentCancelsAndTheFirstWaitingGetsTheSeat() {
        assertThat(cancel(holder.token(), seatOf(holder)).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertTheSeatWentToTheFirstOnly();
    }

    @Test
    void theTeacherCancelsTheBookingAndTheFirstWaitingGetsTheSeat() {
        assertThat(cancel(teacherToken, seatOf(holder)).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertTheSeatWentToTheFirstOnly();
    }

    @Test
    void anAdministratorCancelsTheBookingAndTheFirstWaitingGetsTheSeat() {
        assertThat(cancel(anAdminToken(), seatOf(holder)).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertTheSeatWentToTheFirstOnly();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void raisingTheCapacityByTwoGivesBothWaitingStudentsASeat() {
        UUID group = aLessonStartingIn(IN_A_MONTH.plusDays(2), LessonType.GROUP, 1);
        assertThat(book(holder.token(), group).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        join(first.token(), group);
        join(second.token(), group);

        ResponseEntity<Map> edited = rest.exchange("/api/v1/teacher/lessons/" + group, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("capacity", 3), jsonBearer(teacherToken)), Map.class);

        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(edited.getBody()).containsEntry("bookedCount", 3).containsEntry("status", "FULL");
        assertThat(confirmedBookingsOf(group)).isEqualTo(3);
        assertThat(waitingIn(group)).isZero();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherLetsTheHolderGoAndTheFirstWaitingGetsTheSeat() {
        ResponseEntity<Map> released = rest.exchange("/api/v1/teacher/students/" + holder.id() + "/manage",
                HttpMethod.DELETE, new HttpEntity<>(bearer(teacherToken)), Map.class);
        assertThat(released.getStatusCode().is2xxSuccessful()).isTrue();

        assertTheSeatWentToTheFirstOnly();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void anAdministratorDisablesTheHolderAndTheFirstWaitingGetsTheSeat() {
        ResponseEntity<Map> disabled = rest.exchange("/api/v1/admin/users/" + holder.id() + "/status",
                HttpMethod.PATCH, new HttpEntity<>(Map.of("status", "DISABLED"), jsonBearer(anAdminToken())), Map.class);
        assertThat(disabled.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertTheSeatWentToTheFirstOnly();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theHolderDeletesTheirAccountAndTheFirstWaitingGetsTheSeat() {
        ResponseEntity<Map> deleted = rest.exchange("/api/v1/me/deletion", HttpMethod.POST,
                new HttpEntity<>(Map.of("password", PASSWORD), jsonBearer(holder.token())), Map.class);
        assertThat(deleted.getStatusCode().is2xxSuccessful()).isTrue();

        assertTheSeatWentToTheFirstOnly();
    }

    /** Deleting an account takes the student off every list: nothing of theirs is kept waiting. */
    @Test
    @SuppressWarnings("rawtypes")
    void aWaitingStudentWhoDeletesTheirAccountLeavesTheList() {
        ResponseEntity<Map> deleted = rest.exchange("/api/v1/me/deletion", HttpMethod.POST,
                new HttpEntity<>(Map.of("password", PASSWORD), jsonBearer(first.token())), Map.class);
        assertThat(deleted.getStatusCode().is2xxSuccessful()).isTrue();

        assertThat(waitingIn(lessonId)).isEqualTo(1);
    }

    /** Somebody who can no longer book it is skipped, and leaves the list. */
    @Test
    void aWaitingStudentTheTeacherNoLongerManagesIsSkipped() {
        Student stranger = aStudentWithAProfile();
        jdbc.update("UPDATE waiting_list_entries SET joined_at = joined_at + interval '1 hour' WHERE lesson_id = ?",
                lessonId);
        jdbc.update("INSERT INTO waiting_list_entries (lesson_id, teacher_user_id, student_user_id, joined_at) "
                + "VALUES (?, ?, ?, now() - interval '1 day')", lessonId, teacherId, stranger.id());

        assertThat(cancel(holder.token(), seatOf(holder)).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(hasSeat(stranger, lessonId)).isFalse();
        assertTheSeatWentToTheFirstOnly();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void cancellingTheLessonEmptiesItsListAndBooksNobody() {
        ResponseEntity<Map> cancelled = rest.exchange("/api/v1/teacher/lessons/" + lessonId + "/cancel",
                HttpMethod.POST, new HttpEntity<>(bearer(teacherToken)), Map.class);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(waitingIn(lessonId)).isZero();
        assertThat(confirmedBookingsOf(lessonId)).isZero();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void leavingTheListMovesTheNextOneUp() {
        ResponseEntity<Void> left = rest.exchange("/api/v1/lessons/" + lessonId + "/waiting-list", HttpMethod.DELETE,
                new HttpEntity<>(bearer(first.token())), Void.class);
        assertThat(left.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        List<Map> mine = (List<Map>) get(second.token(), "/api/v1/waiting-list").getBody().get("items");
        assertThat(mine).singleElement().satisfies(place -> assertThat(place).containsEntry("position", 1));
    }

    /** The seat went to the first student waiting, who was told, and the second is now first. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertTheSeatWentToTheFirstOnly() {
        assertThat(hasSeat(first, lessonId)).isTrue();
        assertThat(hasSeat(second, lessonId)).isFalse();
        assertThat(confirmedBookingsOf(lessonId)).isEqualTo(1);

        List<Map> mine = (List<Map>) get(second.token(), "/api/v1/waiting-list").getBody().get("items");
        assertThat(mine).singleElement().satisfies(place -> assertThat(place).containsEntry("position", 1));

        List<Mail> toFirst = mailer.to(first.email());
        assertThat(toFirst).extracting(Mail::subject).anySatisfy(subject -> assertThat(subject).startsWith("Tienes plaza"));
    }

    private boolean hasSeat(Student student, UUID lesson) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM bookings WHERE lesson_id = ? AND student_user_id = ? "
                + "AND status = 'CONFIRMED'", Integer.class, lesson, student.id());
        return count != null && count == 1;
    }

    private int waitingIn(UUID lesson) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM waiting_list_entries WHERE lesson_id = ?",
                Integer.class, lesson);
        return count == null ? 0 : count;
    }

    private UUID seatOf(Student student) {
        return jdbc.queryForObject("SELECT id FROM bookings WHERE lesson_id = ? AND student_user_id = ? "
                + "AND status = 'CONFIRMED'", UUID.class, lessonId, student.id());
    }

    private String anAdminToken() {
        Student admin = aStudentWithAProfile();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", admin.id());
        return tokenOf(admin.email(), PASSWORD);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> join(String token, UUID lesson) {
        return rest.exchange("/api/v1/lessons/" + lesson + "/waiting-list", HttpMethod.POST,
                new HttpEntity<>(bearer(token)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> cancel(String token, UUID booking) {
        return rest.exchange("/api/v1/bookings/" + booking + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(token)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> get(String token, String path) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(bearer(token)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private static UUID bookingOf(ResponseEntity<Map> booked) {
        return UUID.fromString((String) booked.getBody().get("id"));
    }
}
