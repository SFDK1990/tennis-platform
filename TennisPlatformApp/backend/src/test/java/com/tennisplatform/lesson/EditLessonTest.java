package com.tennisplatform.lesson;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.lesson.domain.LessonType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code PATCH /teacher/lessons/{id}}: the notes and the capacity, never the time (30-fase19-analisis-cierre-mvp.md). */
class EditLessonTest extends AbstractBookingTest {

    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherChangesTheNotesAndRaisesTheCapacity() {
        UUID lessonId = aLessonStartingIn(Duration.ofDays(3), LessonType.GROUP, 3);

        ResponseEntity<Map> edited = edit(lessonId, "Traed agua, hace calor", 6);

        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(edited.getBody()).containsEntry("notes", "Traed agua, hace calor").containsEntry("capacity", 6);
        assertThat(edit(lessonId, "  ", null).getBody()).containsEntry("notes", null).containsEntry("capacity", 6);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theCapacityGoesDownToTheBookingsButNotBelow() {
        UUID lessonId = aLessonStartingIn(Duration.ofDays(3), LessonType.GROUP, 4);
        book(aStudentWhoMayBook().token(), lessonId);
        book(aStudentWhoMayBook().token(), lessonId);

        ResponseEntity<Map> below = edit(lessonId, null, 1);
        ResponseEntity<Map> exact = edit(lessonId, null, 2);

        assertThat(below.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(below.getBody()).containsEntry("code", "LESSON_CAPACITY_BELOW_BOOKINGS");
        assertThat(exact.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exact.getBody()).containsEntry("capacity", 2).containsEntry("status", "FULL");
    }

    /** The same rule as creating it: an individual lesson holds one, a group one up to the configured cap. */
    @Test
    @SuppressWarnings("rawtypes")
    void theCapacityMustFitTheKindOfLesson() {
        ResponseEntity<Map> individual = edit(anIndividualLessonIn(Duration.ofDays(3)), null, 2);
        ResponseEntity<Map> tooBig = edit(aLessonStartingIn(Duration.ofDays(4), LessonType.GROUP, 3), null, 9);

        assertThat(individual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(individual.getBody()).containsEntry("code", "LESSON_INVALID");
        assertThat(tooBig.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aLessonThatStartedOrWasCancelledNoLongerChanges() {
        UUID started = anIndividualLessonIn(Duration.ofDays(3));
        jdbc.update("UPDATE lessons SET starts_at = now() - interval '10 minutes', "
                + "ends_at = now() + interval '50 minutes' WHERE id = ?", started);
        UUID cancelled = anIndividualLessonIn(Duration.ofDays(4));
        rest.exchange("/api/v1/teacher/lessons/" + cancelled + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(teacherToken)), Map.class);

        ResponseEntity<Map> afterStart = edit(started, "tarde", null);
        ResponseEntity<Map> afterCancel = edit(cancelled, "tarde", null);

        assertThat(afterStart.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(afterStart.getBody()).containsEntry("code", "LESSON_ALREADY_STARTED");
        assertThat(afterCancel.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(afterCancel.getBody()).containsEntry("code", "LESSON_ALREADY_CANCELLED");
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> edit(UUID lessonId, String notes, Integer capacity) {
        Map<String, Object> body = new HashMap<>();
        if (notes != null) {
            body.put("notes", notes);
        }
        if (capacity != null) {
            body.put("capacity", capacity);
        }
        return rest.exchange("/api/v1/teacher/lessons/" + lessonId, HttpMethod.PATCH,
                new HttpEntity<>(body, jsonBearer(teacherToken)), Map.class);
    }
}
