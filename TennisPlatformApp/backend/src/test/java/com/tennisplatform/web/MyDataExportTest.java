package com.tennisplatform.web;

import com.tennisplatform.booking.AbstractBookingTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code GET /me/export} (01-analisis-funcional.md §4, 30-fase19-analisis-cierre-mvp.md). */
class MyDataExportTest extends AbstractBookingTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void aStudentDownloadsTheirAccountAndEveryBookingOfTheirsAndNobodyElses() {
        Student student = aStudentWhoMayBook();
        Student other = aStudentWhoMayBook();
        UUID first = anIndividualLessonIn(Duration.ofDays(3));
        UUID second = anIndividualLessonIn(Duration.ofDays(4));
        book(student.token(), first);
        book(student.token(), second);
        book(other.token(), anIndividualLessonIn(Duration.ofDays(5)));

        ResponseEntity<Map> export = export(student.token());

        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(export.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment");
        assertThat((Map<String, Object>) export.getBody().get("account"))
                .containsEntry("email", student.email())
                .containsEntry("fullName", "Lucia Prieto");
        List<Map<String, Object>> bookings = (List<Map<String, Object>>) export.getBody().get("bookings");
        assertThat(bookings).extracting(booking -> booking.get("lessonId"))
                .containsExactlyInAnyOrder(first.toString(), second.toString());
        assertThat(bookings).allSatisfy(booking -> {
            assertThat(booking).containsEntry("studentUserId", student.id().toString());
            assertThat((Map<String, Object>) booking.get("lesson")).containsKey("startsAt");
        });
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theTeacherDownloadsTheirAccountWithoutBookings() {
        ResponseEntity<Map> export = export(teacherToken);

        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) export.getBody().get("account")).containsEntry("email", TEACHER_EMAIL);
        assertThat((List<?>) export.getBody().get("bookings")).isEmpty();
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> export(String token) {
        return rest.exchange("/api/v1/me/export", HttpMethod.GET, new HttpEntity<>(bearer(token)), Map.class);
    }
}
