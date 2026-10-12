package com.tennisplatform.lesson;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.lesson.application.port.in.CancelLesson;
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
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/** {@code POST /teacher/lessons/cancellations} (32-fase20-analisis-avisos-calendario.md, decision 4). */
class CancelDayTest extends AbstractBookingTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final LocalDate RAINY_DAY = LocalDate.parse("2027-05-12");

    @Autowired
    private RecordingMailer mailer;

    @Autowired
    private CancelLesson cancelLesson;

    @BeforeEach
    void clearTheMailbox() {
        mailer.clear();
    }

    private static Instant at(LocalDate day, int hour) {
        return day.atTime(hour, 0).atZone(MADRID).toInstant();
    }

    @Test
    @SuppressWarnings("unchecked")
    void theDayIsCancelledWithEveryBookingAndEachStudentHearsWhy() {
        Student lucia = aStudentWhoMayBook();
        Student pablo = aStudentWhoMayBook();
        UUID morning = aLessonAt(at(RAINY_DAY, 10), LessonType.INDIVIDUAL, 1);
        UUID noon = aLessonAt(at(RAINY_DAY, 12), LessonType.INDIVIDUAL, 1);
        UUID nextDay = aLessonAt(at(RAINY_DAY.plusDays(1), 10), LessonType.INDIVIDUAL, 1);
        book(lucia.token(), morning);
        book(pablo.token(), noon);
        mailer.clear();

        ResponseEntity<Map> response = cancelDay(RAINY_DAY.toString(), "Lluvia");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<Map<String, Object>>) response.getBody().get("lessons"))
                .extracting(lesson -> lesson.get("id"), lesson -> lesson.get("status"))
                .containsExactlyInAnyOrder(
                        tuple(morning.toString(), "CANCELLED"),
                        tuple(noon.toString(), "CANCELLED"));
        assertThat(confirmedBookingsOf(morning) + confirmedBookingsOf(noon)).isZero();
        assertThat(cancelledAtOf(nextDay)).isNull();

        for (Student student : List.of(lucia, pablo)) {
            assertThat(mailer.to(student.email())).singleElement().satisfies(mail -> {
                assertThat(mail.kind()).isEqualTo("booking-cancelled-student");
                assertThat(mail.body()).contains("Motivo: Lluvia");
            });
        }
        assertThat(mailer.to(TEACHER_EMAIL)).isEmpty();
    }

    @Test
    void cancellingASingleLessonCarriesTheReasonToo() {
        Student lucia = aStudentWhoMayBook();
        UUID lesson = aLessonAt(at(RAINY_DAY, 10), LessonType.INDIVIDUAL, 1);
        book(lucia.token(), lesson);
        mailer.clear();

        ResponseEntity<Map> response = rest.exchange("/api/v1/teacher/lessons/" + lesson + "/cancel", HttpMethod.POST,
                new HttpEntity<>(Map.of("reason", "Estoy enfermo"), jsonBearer(teacherToken)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mailer.to(lucia.email())).singleElement()
                .satisfies(mail -> assertThat(mail.body()).contains("Motivo: Estoy enfermo"));
    }

    @Test
    void aDayWithoutLessonsAnswersAnEmptyList() {
        ResponseEntity<Map> response = cancelDay(RAINY_DAY.toString(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("lessons", List.of());
    }

    @Test
    void aDayGoneByIsRefused() {
        ResponseEntity<Map> response = cancelDay(LocalDate.now(MADRID).minusDays(1).toString(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).containsEntry("code", "DATE_IN_THE_PAST");
    }

    @Test
    void aReasonLongerThan200CharactersIsRefused() {
        ResponseEntity<Map> response = cancelDay(RAINY_DAY.toString(), "x".repeat(201));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * The second lesson of the day cannot be cancelled - a trigger refuses it - and the first is
     * left as it was: the day goes whole or not at all. Called on the use case, because the
     * failure answers a 500 that the contract rightly does not describe.
     */
    @Test
    void whenOneLessonOfTheDayCannotBeCancelledNoneIs() {
        UUID first = aLessonAt(at(RAINY_DAY, 10), LessonType.GROUP, 4);
        UUID second = aLessonAt(at(RAINY_DAY, 12), LessonType.GROUP, 4);
        jdbc.execute("""
                CREATE FUNCTION refuse_lesson_update() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'refused by the test'; END $$ LANGUAGE plpgsql""");
        jdbc.execute("CREATE TRIGGER refuse_second BEFORE UPDATE ON lessons FOR EACH ROW WHEN (OLD.id = '"
                + second + "') EXECUTE FUNCTION refuse_lesson_update()");
        try {
            assertThatThrownBy(() -> cancelLesson.cancelDay(teacherId, RAINY_DAY, null)).isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER refuse_second ON lessons");
            jdbc.execute("DROP FUNCTION refuse_lesson_update()");
        }

        assertThat(cancelledAtOf(first)).isNull();
        assertThat(cancelledAtOf(second)).isNull();
    }

    /** When the lesson was cancelled, or null while it still runs. */
    private Instant cancelledAtOf(UUID lessonId) {
        return jdbc.queryForObject("SELECT cancelled_at FROM lessons WHERE id = ?", Instant.class, lessonId);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> cancelDay(String date, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("date", date);
        if (reason != null) {
            body.put("reason", reason);
        }
        return rest.exchange("/api/v1/teacher/lessons/cancellations", HttpMethod.POST,
                new HttpEntity<>(body, jsonBearer(teacherToken)), Map.class);
    }
}
