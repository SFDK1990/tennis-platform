package com.tennisplatform.lesson;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.lesson.domain.LessonType;
import com.tennisplatform.notification.RecordingMailer;
import com.tennisplatform.notification.application.port.out.Mail;
import org.junit.jupiter.api.AfterEach;
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
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two courts (40-fase25.6-analisis-pistas.md). The courts are not emptied between tests, like
 * the configuration: a test that renames one puts the name back.
 */
class CourtsApiTest extends AbstractBookingTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    @Autowired
    private RecordingMailer mailer;

    private String first;
    private String second;
    private LocalDate day;

    @BeforeEach
    @SuppressWarnings("rawtypes")
    void theTwoCourts() {
        List<Map> courts = courts();
        assertThat(courts).extracting(court -> court.get("name")).containsExactly("Pista 1", "Pista 2");
        first = (String) courts.get(0).get("id");
        second = (String) courts.get(1).get("id");
        day = LocalDate.now(MADRID).plusDays(10);
    }

    @AfterEach
    void theNamesGoBack() {
        jdbc.update("UPDATE courts SET name = 'Pista ' || position");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void aLessonGoesOnTheCourtItIsCreatedOnAndOnTheFirstOneOtherwise() {
        assertThat(create(10, null).getBody()).containsEntry("courtId", first);
        UUID onTheSecond = UUID.fromString((String) create(12, second).getBody().get("id"));
        UUID onTheRepository = aLessonStartingIn(Duration.ofDays(20), LessonType.GROUP, 4);
        assertThat(jdbc.queryForObject("SELECT court_id::text FROM lessons WHERE id = ?", String.class,
                onTheRepository)).isEqualTo(first);

        Student student = aStudentWhoMayBook();
        ResponseEntity<Map> booked = book(student.token(), onTheSecond);
        assertThat((Map) booked.getBody().get("lesson")).containsEntry("courtId", second);
        Map calendar = rest.exchange("/api/v1/calendar?from=" + day + "&to=" + day, HttpMethod.GET,
                new HttpEntity<>(bearer(student.token())), Map.class).getBody();
        assertThat((List<Map>) calendar.get("lessons")).filteredOn(lesson -> lesson.get("courtId").equals(second))
                .hasSize(1);

        ResponseEntity<Map> unknown = create(14, UUID.randomUUID().toString());
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknown.getBody()).containsEntry("code", "COURT_NOT_FOUND");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void aWholeSeriesGoesOnItsCourt() {
        Map<String, Object> body = lessonBody(17, second);
        body.put("weeks", 3);
        List<Map> occurrences = (List<Map>) rest.exchange("/api/v1/teacher/lessons/series/preview", HttpMethod.POST,
                new HttpEntity<>(body, jsonBearer(teacherToken)), Map.class).getBody().get("occurrences");
        body.put("dates", occurrences.stream().map(occurrence -> occurrence.get("startsAt")).toList());

        List<Map> lessons = (List<Map>) rest.exchange("/api/v1/teacher/lessons/series", HttpMethod.POST,
                new HttpEntity<>(body, jsonBearer(teacherToken)), Map.class).getBody().get("lessons");

        assertThat(lessons).hasSize(3).extracting(lesson -> lesson.get("courtId")).containsOnly(second);
    }

    /** One teacher cannot give two lessons at once, whatever the courts. */
    @Test
    void twoLessonsAtTheSameTimeStillClashOnDifferentCourts() {
        create(10, first);
        ResponseEntity<Map> clash = create(10, second);
        assertThat(clash.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    /** Decision 3: whoever holds a seat hears about it; whoever cancelled does not. */
    @Test
    @SuppressWarnings("rawtypes")
    void movingALessonTellsTheStudentsWithASeatAndNobodyElse() {
        UUID lesson = UUID.fromString((String) create(18, null).getBody().get("id"));
        Student seated = aStudentWhoMayBook();
        Student cancelled = aStudentWhoMayBook();
        book(seated.token(), lesson);
        UUID booking = UUID.fromString((String) book(cancelled.token(), lesson).getBody().get("id"));
        rest.exchange("/api/v1/bookings/" + booking + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(cancelled.token())), Map.class);
        mailer.clear();

        ResponseEntity<Map> moved = move(lesson, second);

        assertThat(moved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(moved.getBody()).containsEntry("courtId", second);
        assertThat(mailer.to(seated.email())).singleElement().satisfies(mail -> {
            assertThat(mail.kind()).isEqualTo("lesson-court-changed-student");
            assertThat(mail.subject()).startsWith("Cambio de pista: ");
            assertThat(mail.body()).contains("pasa a la Pista 2. La hora no cambia.");
        });
        assertThat(mailer.sent()).extracting(Mail::to).containsOnly(seated.email());

        mailer.clear();
        move(lesson, second);
        assertThat(mailer.sent()).isEmpty();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aLessonThatStartedOrWasCancelledStaysWhereItIs() {
        UUID started = aLessonStartingIn(Duration.ofMinutes(-30), LessonType.GROUP, 4);
        ResponseEntity<Map> refused = move(started, second);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(refused.getBody()).containsEntry("code", "LESSON_ALREADY_STARTED");

        String lesson = (String) create(9, null).getBody().get("id");
        rest.exchange("/api/v1/teacher/lessons/" + lesson + "/cancel", HttpMethod.POST,
                new HttpEntity<>(jsonBearer(teacherToken)), Map.class);
        assertThat(move(UUID.fromString(lesson), second).getBody()).containsEntry("code", "LESSON_ALREADY_CANCELLED");
        assertThat(move(UUID.fromString(lesson), UUID.randomUUID().toString()).getBody())
                .containsEntry("code", "COURT_NOT_FOUND");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherRenamesACourtAndOnlyTheTeacher() {
        ResponseEntity<Map> renamed = rename(second, "Pista central", teacherToken);
        assertThat(renamed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(courts()).extracting(court -> court.get("name")).containsExactly("Pista 1", "Pista central");

        assertThat(rename(first, "Pista central", teacherToken).getBody()).containsEntry("code", "COURT_ALREADY_EXISTS");
        assertThat(rename(first, " ", teacherToken).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(rename(first, "Mía", aStudentWhoMayBook().token()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // --- helpers -------------------------------------------------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Map> courts() {
        return (List<Map>) rest.exchange("/api/v1/courts", HttpMethod.GET, new HttpEntity<>(bearer(teacherToken)),
                Map.class).getBody().get("courts");
    }

    private Map<String, Object> lessonBody(int hour, String courtId) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "GROUP");
        body.put("startsAt", day.atTime(LocalTime.of(hour, 0)).atZone(MADRID).toInstant().toString());
        body.put("endsAt", day.atTime(LocalTime.of(hour + 1, 0)).atZone(MADRID).toInstant().toString());
        body.put("capacity", 4);
        body.put("overrideAvailability", true);
        body.put("courtId", courtId);
        return body;
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> create(int hour, String courtId) {
        return rest.exchange("/api/v1/teacher/lessons", HttpMethod.POST,
                new HttpEntity<>(lessonBody(hour, courtId), jsonBearer(teacherToken)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> move(UUID lesson, String courtId) {
        return rest.exchange("/api/v1/teacher/lessons/" + lesson + "/court", HttpMethod.PUT,
                new HttpEntity<>(Map.of("courtId", courtId), jsonBearer(teacherToken)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> rename(String court, String name, String token) {
        return rest.exchange("/api/v1/teacher/courts/" + court, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("name", name), jsonBearer(token)), Map.class);
    }
}
