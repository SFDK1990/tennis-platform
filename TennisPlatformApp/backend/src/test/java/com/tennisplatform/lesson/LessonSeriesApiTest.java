package com.tennisplatform.lesson;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.lesson.domain.LessonType;
import com.tennisplatform.notification.RecordingMailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Weekly series over real HTTP (35-fase22-analisis-clases-que-se-repiten.md). */
class LessonSeriesApiTest extends AbstractBookingTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final String SERIES = "/api/v1/teacher/lessons/series";

    @Autowired
    private RecordingMailer mailer;

    private LocalDate firstDay;

    @BeforeEach
    void aWorkingDayInAMonth() {
        firstDay = LocalDate.now(clock.withZone(MADRID)).plusDays(30);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void thePreviewSaysWhatIsWrongWithEachDate() {
        openTheWeekday();
        block(firstDay.plusWeeks(1));
        aLessonAt(week(2), LessonType.INDIVIDUAL, 1);

        ResponseEntity<Map> preview = post(SERIES + "/preview", series(4));

        assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<Map>) preview.getBody().get("occurrences")).extracting(o -> o.get("status"))
                .containsExactly("OK", "OUTSIDE_AVAILABILITY", "OVERLAP", "OK");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void aFirstDateInThePastIsMarkedSo() {
        Map<String, Object> body = series(2);
        Instant past = LocalDate.now(clock.withZone(MADRID)).minusDays(1).atTime(LocalTime.of(10, 0)).atZone(MADRID)
                .toInstant();
        body.put("startsAt", past.toString());
        body.put("endsAt", past.plus(1, ChronoUnit.HOURS).toString());

        ResponseEntity<Map> preview = post(SERIES + "/preview", body);

        assertThat((List<Map>) preview.getBody().get("occurrences")).extracting(o -> o.get("status"))
                .first().isEqualTo("IN_THE_PAST");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void createsOnlyTheChosenDatesAsOneSeries() {
        openTheWeekday();
        block(firstDay.plusWeeks(1));
        Map<String, Object> body = series(4);
        body.put("dates", List.of(week(0).toString(), week(1).toString(), week(3).toString()));

        ResponseEntity<Map> outside = post(SERIES, body);
        assertThat(outside.getBody()).containsEntry("code", "LESSON_OUTSIDE_AVAILABILITY");
        assertThat(lessonsInSeries()).isZero();

        body.put("overrideAvailability", true);
        ResponseEntity<Map> created = post(SERIES, body);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        List<Map> lessons = (List<Map>) created.getBody().get("lessons");
        assertThat(lessons).hasSize(3).extracting(l -> l.get("seriesId")).containsOnly(lessons.getFirst().get("seriesId"));
        assertThat(lessons).extracting(l -> l.get("createdOutsideAvailability")).containsExactly(false, true, false);

        ResponseEntity<Map> last = rest.exchange("/api/v1/lessons/" + lessons.get(2).get("id"), HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
        assertThat(last.getBody()).containsEntry("seriesPosition", 3).containsEntry("seriesSize", 3);
    }

    /** All or nothing: a chosen date that clashes leaves no lesson of the series behind. */
    @Test
    @SuppressWarnings("rawtypes")
    void aChosenDateThatClashesCreatesNoneOfThem() {
        aLessonAt(week(2), LessonType.INDIVIDUAL, 1);
        Map<String, Object> body = series(4);
        body.put("overrideAvailability", true);
        body.put("dates", List.of(week(0).toString(), week(1).toString(), week(2).toString()));

        ResponseEntity<Map> refused = post(SERIES, body);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refused.getBody()).containsEntry("code", "LESSON_OVERLAP");
        assertThat(lessonsInSeries()).isZero();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void refusesADateOutsideTheSeriesAndMoreThanSixteenWeeks() {
        Map<String, Object> body = series(4);
        body.put("overrideAvailability", true);
        body.put("dates", List.of(week(0).plus(1, ChronoUnit.DAYS).toString()));
        assertThat(post(SERIES, body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(post(SERIES + "/preview", series(17)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void cancellingFromOneLessonCancelsItAndTheFollowingOnesAndTellsTheirStudents() {
        Map<String, Object> body = series(4);
        body.put("overrideAvailability", true);
        body.put("dates", List.of(week(0).toString(), week(1).toString(), week(2).toString(), week(3).toString()));
        List<Map> lessons = (List<Map>) post(SERIES, body).getBody().get("lessons");
        Student student = aStudentWhoMayBook();
        assertThat(book(student.token(), UUID.fromString((String) lessons.get(2).get("id"))).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        mailer.clear();

        ResponseEntity<Map> cancelled = post("/api/v1/teacher/lessons/" + lessons.get(1).get("id") + "/cancel-following",
                Map.of("reason", "Fin del curso"));

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<Map>) cancelled.getBody().get("lessons")).extracting(l -> l.get("id"))
                .containsExactly(lessons.get(1).get("id"), lessons.get(2).get("id"), lessons.get(3).get("id"));
        assertThat(jdbc.queryForObject("SELECT cancelled_at IS NULL FROM lessons WHERE id = ?::uuid", Boolean.class,
                lessons.getFirst().get("id"))).isTrue();
        assertThat(confirmedBookingsOf(UUID.fromString((String) lessons.get(2).get("id")))).isZero();
        assertThat(mailer.to(student.email())).anySatisfy(mail -> assertThat(mail.body()).contains("Fin del curso"));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aLessonCreatedOnItsOwnHasNoFollowingOnes() {
        UUID single = aLessonAt(week(0), LessonType.INDIVIDUAL, 1);

        ResponseEntity<Map> refused = post("/api/v1/teacher/lessons/" + single + "/cancel-following", Map.of());

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(refused.getBody()).containsEntry("code", "LESSON_NOT_IN_SERIES");
    }

    private Instant week(int week) {
        return firstDay.plusWeeks(week).atTime(LocalTime.of(10, 0)).atZone(MADRID).toInstant();
    }

    private Map<String, Object> series(int weeks) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "INDIVIDUAL");
        body.put("capacity", 1);
        body.put("weeks", weeks);
        body.put("startsAt", week(0).toString());
        body.put("endsAt", week(0).plus(1, ChronoUnit.HOURS).toString());
        return body;
    }

    private int lessonsInSeries() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM lessons WHERE series_id IS NOT NULL", Integer.class);
        return count == null ? 0 : count;
    }

    @SuppressWarnings("rawtypes")
    private void openTheWeekday() {
        ResponseEntity<Map> response = rest.exchange("/api/v1/teacher/availability/weekly", HttpMethod.PUT,
                new HttpEntity<>(Map.of("rules", List.of(Map.of("dayOfWeek", firstDay.getDayOfWeek().name(),
                        "startTime", "09:00:00", "endTime", "19:00:00"))), jsonBearer(teacherToken)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @SuppressWarnings("rawtypes")
    private void block(LocalDate day) {
        ResponseEntity<Map> response = post("/api/v1/teacher/availability/exceptions",
                Map.of("date", day.toString(), "type", "BLOCK"));
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> post(String path, Map<String, Object> body) {
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, jsonBearer(teacherToken)), Map.class);
    }
}
