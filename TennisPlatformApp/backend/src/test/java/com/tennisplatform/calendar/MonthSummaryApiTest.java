package com.tennisplatform.calendar;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.lesson.domain.LessonType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The summary through the API, against the real clock (Fase 25). The counting itself, with its
 * edge cases on literal dates, is {@code GetMonthSummaryServiceTest}.
 */
class MonthSummaryApiTest extends AbstractBookingTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherSeesALessonGivenThisMonthAndAStudentWhoDeletedTheirAccountStillCounts() {
        Instant monthStarts = YearMonth.now(MADRID).atDay(1).atStartOfDay(MADRID).toInstant();
        UUID lesson = aLessonStartingIn(Duration.ofDays(3), LessonType.GROUP, 4);
        Student student = aStudentWhoMayBook();
        Student leaving = aStudentWhoMayBook();
        book(student.token(), lesson);
        book(leaving.token(), lesson);
        // Moved to the start of the month, which has always passed: the lesson is now one given.
        Instant startsAt = monthStarts.plus(Duration.ofMinutes(5)).truncatedTo(ChronoUnit.MINUTES);
        jdbc.update("UPDATE lessons SET starts_at = ?::timestamptz, ends_at = ?::timestamptz WHERE id = ?",
                startsAt.toString(), startsAt.plus(Duration.ofHours(1)).toString(), lesson);
        jdbc.update("UPDATE bookings SET lesson_starts_at = ?::timestamptz, lesson_ends_at = ?::timestamptz "
                + "WHERE lesson_id = ?", startsAt.toString(), startsAt.plus(Duration.ofHours(1)).toString(), lesson);
        rest.exchange("/api/v1/me/deletion", HttpMethod.POST,
                new HttpEntity<>(Map.of("password", PASSWORD), jsonBearer(leaving.token())), Map.class);

        ResponseEntity<Map> summary = summary("");

        assertThat(summary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(summary.getBody()).containsEntry("month", YearMonth.now(MADRID).toString());
        Map current = (Map) summary.getBody().get("current");
        assertThat(current).containsEntry("lessonsGiven", 1).containsEntry("seatsOffered", 4)
                .containsEntry("seatsTaken", 2).containsEntry("unmarked", 2).containsEntry("activeStudents", 2);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aMonthStillToComeOrWrittenWrongIsRefused() {
        String next = YearMonth.now(MADRID).plusMonths(1).toString();
        assertThat(summary("?month=" + next).getBody()).containsEntry("code", "DATE_RANGE_INVALID");
        assertThat(summary("?month=octubre").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(summary("?month=" + YearMonth.now(MADRID).minusMonths(3)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> summary(String query) {
        return rest.exchange("/api/v1/teacher/summary" + query, HttpMethod.GET, new HttpEntity<>(bearer(teacherToken)),
                Map.class);
    }
}
