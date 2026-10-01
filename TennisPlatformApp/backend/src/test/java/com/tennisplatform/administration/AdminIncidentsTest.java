package com.tennisplatform.administration;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.lesson.domain.LessonType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The administrator resolving an incident (01-analisis-funcional.md §17,
 * 30-fase19-analisis-cierre-mvp.md): sees the diary and every booking, and cancels a lesson
 * without the teacher.
 */
class AdminIncidentsTest extends AbstractBookingTest {

    private String adminToken;
    private Student student;
    private UUID lessonId;

    @BeforeEach
    void anAdminAndAGroupLessonWithABooking() {
        Student admin = aStudentWithAProfile();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", admin.id());
        adminToken = tokenOf(admin.email(), PASSWORD);

        student = aStudentWhoMayBook();
        lessonId = aLessonStartingIn(Duration.ofDays(3), LessonType.GROUP, 4);
        assertThat(book(student.token(), lessonId).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theAdministratorSeesTheTeachersDiaryAndEveryBooking() {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Madrid"));
        ResponseEntity<Map> calendar = get("/api/v1/calendar?from=" + today + "&to=" + today.plusDays(7));
        ResponseEntity<Map> bookings = get("/api/v1/bookings?lessonId=" + lessonId);

        assertThat(calendar.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<Map<String, Object>>) calendar.getBody().get("lessons"))
                .extracting(lesson -> lesson.get("id")).contains(lessonId.toString());
        assertThat((List<Map<String, Object>>) bookings.getBody().get("items"))
                .singleElement()
                .satisfies(booking -> assertThat(booking).containsEntry("studentUserId", student.id().toString()));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void cancellingALessonLeavesItsBookingsCancelledByTheAdministration() {
        ResponseEntity<Map> cancelled = cancel(adminToken, lessonId);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody()).containsEntry("status", "CANCELLED");
        assertThat(jdbc.queryForObject("SELECT status FROM bookings WHERE lesson_id = ?", String.class, lessonId))
                .isEqualTo("CANCELLED_BY_ADMIN");
        assertThat(cancel(adminToken, lessonId).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    /** The prefix says who may call it: the teacher cancels through their own route. */
    @Test
    void theTeacherCannotUseTheAdministrationsRoute() {
        assertThat(cancel(teacherToken, lessonId).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(confirmedBookingsOf(lessonId)).isEqualTo(1);
    }

    @Test
    void aLessonThatDoesNotExistAnswersNotFound() {
        assertThat(cancel(adminToken, UUID.randomUUID()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> get(String path) {
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(bearer(adminToken)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> cancel(String token, UUID lesson) {
        return rest.exchange("/api/v1/admin/lessons/" + lesson + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(token)), Map.class);
    }
}
