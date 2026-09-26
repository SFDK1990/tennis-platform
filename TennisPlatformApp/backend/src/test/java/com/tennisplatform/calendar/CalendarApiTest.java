package com.tennisplatform.calendar;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.lesson.domain.LessonType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Criteria of 21-fase10-analisis-calendar.md, over real HTTP. */
class CalendarApiTest extends AbstractBookingTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    @Test
    @SuppressWarnings("unchecked")
    void theTeacherSeesTheirHoursAndEveryLessonCancelledOnesIncluded() {
        openEveryDay();
        UUID kept = anIndividualLessonIn(Duration.ofDays(3));
        UUID cancelled = anIndividualLessonIn(Duration.ofDays(4));
        cancelLesson(cancelled);

        Map<String, Object> calendar = calendar(teacherToken, today(), today().plusDays(10)).getBody();

        assertThat(calendar).containsEntry("timezone", "Europe/Madrid");
        assertThat((List<?>) calendar.get("availability")).isNotEmpty();
        assertThat(idsOf(calendar)).containsExactlyInAnyOrder(kept.toString(), cancelled.toString());
    }

    /** The student sees what they can book, and their own seat where they have one. */
    @Test
    @SuppressWarnings("unchecked")
    void aManagedStudentSeesTheLessonsAndTheirOwnBooking() {
        Student student = aStudentWhoMayBook();
        UUID booked = aLessonStartingIn(Duration.ofDays(3), LessonType.GROUP, 4);
        UUID free = anIndividualLessonIn(Duration.ofDays(5));
        book(student.token(), booked);

        Map<String, Object> calendar = calendar(student.token(), today(), today().plusDays(10)).getBody();

        assertThat((List<?>) calendar.get("availability")).isEmpty();
        List<Map<String, Object>> lessons = (List<Map<String, Object>>) calendar.get("lessons");
        assertThat(idsOf(calendar)).containsExactly(booked.toString(), free.toString());
        assertThat((Map<String, Object>) lessons.get(0).get("myBooking")).containsEntry("status", "CONFIRMED");
        assertThat(lessons.get(1).get("myBooking")).isNull();
    }

    /** A cancelled lesson disappears for the student, unless they had a seat in it: then they need to know why. */
    @Test
    void aStudentSeesACancelledLessonOnlyIfTheyHadASeatInIt() {
        Student student = aStudentWhoMayBook();
        UUID theirs = anIndividualLessonIn(Duration.ofDays(3));
        UUID notTheirs = anIndividualLessonIn(Duration.ofDays(4));
        book(student.token(), theirs);
        cancelLesson(theirs);
        cancelLesson(notTheirs);

        Map<String, Object> calendar = calendar(student.token(), today(), today().plusDays(10)).getBody();

        assertThat(idsOf(calendar)).containsExactly(theirs.toString());
    }

    @Test
    void aStudentNobodyManagesSeesAnEmptyCalendar() {
        Student student = aStudentWithAProfile();
        anIndividualLessonIn(Duration.ofDays(3));

        Map<String, Object> calendar = calendar(student.token(), today(), today().plusDays(10)).getBody();

        assertThat(idsOf(calendar)).isEmpty();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aRangeWiderThanSixtyTwoDaysIsRefused() {
        ResponseEntity<Map> refused = calendar(teacherToken, today(), today().plusDays(62));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(refused.getBody()).containsEntry("code", "DATE_RANGE_INVALID");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void anAdminHasNoCalendar() {
        Student account = aStudentWithAProfile();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", account.id());

        ResponseEntity<Map> refused = calendar(tokenOf(account.email(), PASSWORD), today(), today());

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refused.getBody()).containsEntry("code", "AUTH_FORBIDDEN");
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(MADRID));
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> calendar(String token, LocalDate from, LocalDate to) {
        return rest.exchange("/api/v1/calendar?from=" + from + "&to=" + to, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), Map.class);
    }

    private void cancelLesson(UUID lessonId) {
        rest.exchange("/api/v1/teacher/lessons/" + lessonId + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
    }

    private void openEveryDay() {
        List<Map<String, String>> rules = Arrays.stream(DayOfWeek.values())
                .map(day -> Map.of("dayOfWeek", day.name(), "startTime", "09:00:00", "endTime", "19:00:00"))
                .toList();
        rest.exchange("/api/v1/teacher/availability/weekly", HttpMethod.PUT,
                new HttpEntity<>(Map.of("rules", rules), jsonBearer(teacherToken)), Map.class);
    }

    @SuppressWarnings("unchecked")
    private static List<String> idsOf(Map<String, Object> calendar) {
        return ((List<Map<String, Object>>) calendar.get("lessons")).stream()
                .map(lesson -> (String) lesson.get("id"))
                .toList();
    }
}
