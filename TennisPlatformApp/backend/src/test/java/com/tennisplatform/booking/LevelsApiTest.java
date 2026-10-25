package com.tennisplatform.booking;

import com.tennisplatform.lesson.domain.LessonType;
import com.tennisplatform.teacher.application.port.in.QueryLevels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Levels, who may book what, and the student's record (36-fase23-analisis-niveles-ficha.md). */
class LevelsApiTest extends AbstractBookingTest {

    private static final Duration IN_A_MONTH = Duration.ofDays(30);

    @Autowired
    private QueryLevels queryLevels;

    private String beginners;
    private String advanced;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theTeacherStartsWithTheFourLevels() {
        queryLevels.seedDefaults(teacherId);
        List<Map> levels = levels();
        beginners = (String) levels.get(0).get("id");
        advanced = (String) levels.get(2).get("id");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theFourLevelsComeInOrderAndTheTeacherRenamesAddsAndReordersThem() {
        assertThat(levels()).extracting(level -> level.get("name"))
                .containsExactly("Iniciación", "Intermedio", "Avanzado", "Competición");

        ResponseEntity<Map> created = call(HttpMethod.POST, "/api/v1/teacher/levels", Map.of("name", "Profesional"));
        assertThat(created.getBody()).containsEntry("position", 5);
        assertThat(call(HttpMethod.POST, "/api/v1/teacher/levels", Map.of("name", "Profesional")).getBody())
                .containsEntry("code", "LEVEL_ALREADY_EXISTS");

        call(HttpMethod.PATCH, "/api/v1/teacher/levels/" + created.getBody().get("id"),
                Map.of("name", "Pro", "position", 1));

        assertThat(levels()).extracting(level -> level.get("name"))
                .containsExactly("Pro", "Iniciación", "Intermedio", "Avanzado", "Competición");
        assertThat(levels()).extracting(level -> level.get("position")).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aLevelInUseCannotBeDeletedAndAnUnusedOneCan() {
        Student student = aStudentWhoMayBook();
        record(student, advanced, null, null);

        ResponseEntity<Map> refused = call(HttpMethod.DELETE, "/api/v1/teacher/levels/" + advanced, null);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refused.getBody()).containsEntry("code", "LEVEL_IN_USE");

        UUID lesson = aLessonStartingIn(IN_A_MONTH, LessonType.GROUP, 4);
        onLevel(lesson, beginners);
        assertThat(call(HttpMethod.DELETE, "/api/v1/teacher/levels/" + beginners, null).getBody())
                .containsEntry("code", "LEVEL_IN_USE");

        String unused = (String) levels().get(3).get("id");
        assertThat(call(HttpMethod.DELETE, "/api/v1/teacher/levels/" + unused, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(levels()).hasSize(3);
    }

    /** Decision 4 and 5: one test per row of "who books what". */
    @Test
    @SuppressWarnings("rawtypes")
    void aStudentBooksAnOpenLessonOrOneOfTheirLevelAndNotAnotherOne() {
        Student student = aStudentWhoMayBook();
        record(student, beginners, null, null);
        UUID open = aLessonStartingIn(IN_A_MONTH, LessonType.GROUP, 4);
        UUID ofTheirLevel = aLessonStartingIn(IN_A_MONTH.plusDays(1), LessonType.GROUP, 4);
        UUID ofAnother = aLessonStartingIn(IN_A_MONTH.plusDays(2), LessonType.GROUP, 4);
        onLevel(ofTheirLevel, beginners);
        onLevel(ofAnother, advanced);

        assertThat(book(student.token(), open).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(book(student.token(), ofTheirLevel).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseEntity<Map> refused = book(student.token(), ofAnother);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(refused.getBody()).containsEntry("code", "LEVEL_NOT_ALLOWED");

        Student withoutLevel = aStudentWhoMayBook();
        assertThat(book(withoutLevel.token(), ofAnother).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theWaitingListKeepsToTheLevelToo() {
        UUID lesson = aLessonStartingIn(IN_A_MONTH, LessonType.INDIVIDUAL, 1);
        onLevel(lesson, advanced);
        Student holder = aStudentWhoMayBook();
        Student otherLevel = aStudentWhoMayBook();
        Student sameLevel = aStudentWhoMayBook();
        UUID seat = UUID.fromString((String) book(holder.token(), lesson).getBody().get("id"));
        join(otherLevel, lesson);
        join(sameLevel, lesson);
        record(otherLevel, beginners, null, null);
        record(sameLevel, advanced, null, null);

        ResponseEntity<Map> refused = rest.exchange("/api/v1/lessons/" + lesson + "/waiting-list", HttpMethod.POST,
                new HttpEntity<>(bearer(aStudentOn(beginners).token())), Map.class);
        assertThat(refused.getBody()).containsEntry("code", "LEVEL_NOT_ALLOWED");

        rest.exchange("/api/v1/bookings/" + seat + "/cancel", HttpMethod.POST, new HttpEntity<>(bearer(holder.token())),
                Map.class);

        assertThat(hasSeat(otherLevel, lesson)).isFalse();
        assertThat(hasSeat(sameLevel, lesson)).isTrue();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherWritesTheRecordAndReadsTheHistory() {
        Student student = aStudentWhoMayBook();
        Student other = aStudentWhoMayBook();
        UUID lesson = aLessonStartingIn(IN_A_MONTH, LessonType.GROUP, 4);
        book(student.token(), lesson);
        book(other.token(), lesson);

        ResponseEntity<Map> saved = record(student, advanced, "Mejorar el revés", "Le cuesta el saque");

        assertThat(saved.getBody()).containsEntry("levelId", advanced).containsEntry("goals", "Mejorar el revés")
                .containsEntry("privateNotes", "Le cuesta el saque");
        assertThat(record(student, UUID.randomUUID().toString(), null, null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map> history = rest.exchange("/api/v1/bookings?studentUserId=" + student.id(), HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
        assertThat((List<?>) history.getBody().get("items")).hasSize(1);

        ResponseEntity<Map> me = rest.exchange("/api/v1/me", HttpMethod.GET, new HttpEntity<>(bearer(student.token())),
                Map.class);
        assertThat(me.getBody()).containsEntry("levelId", advanced);
    }

    /** Decision 7: the record leaves with the account, and the student's export carries it. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theExportCarriesTheRecordAndDeletingTheAccountEmptiesIt() {
        Student student = aStudentWhoMayBook();
        record(student, advanced, "Mejorar el revés", "Le cuesta el saque");

        ResponseEntity<Map> export = rest.exchange("/api/v1/me/export", HttpMethod.GET,
                new HttpEntity<>(bearer(student.token())), Map.class);
        assertThat((List<Map>) export.getBody().get("records")).singleElement().satisfies(record -> {
            assertThat(record).containsEntry("level", "Avanzado").containsEntry("goals", "Mejorar el revés")
                    .containsEntry("privateNotes", "Le cuesta el saque");
        });

        rest.exchange("/api/v1/me/deletion", HttpMethod.POST,
                new HttpEntity<>(Map.of("password", PASSWORD), jsonBearer(student.token())), Map.class);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT level_id, goals, private_notes FROM teacher_students WHERE student_user_id = ?", student.id());
        assertThat(row.values()).containsOnlyNulls();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theLessonShowsItsLevelAndCanBeOpenedToEverybody() {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "GROUP");
        body.put("capacity", 4);
        body.put("startsAt", clock.instant().plus(IN_A_MONTH).truncatedTo(java.time.temporal.ChronoUnit.HOURS).toString());
        body.put("endsAt", clock.instant().plus(IN_A_MONTH).truncatedTo(java.time.temporal.ChronoUnit.HOURS)
                .plus(Duration.ofHours(1)).toString());
        body.put("overrideAvailability", true);
        body.put("levelId", advanced);
        ResponseEntity<Map> created = call(HttpMethod.POST, "/api/v1/teacher/lessons", body);
        assertThat(created.getBody()).containsEntry("levelId", advanced);

        Map<String, Object> open = new HashMap<>();
        open.put("levelId", null);
        ResponseEntity<Map> opened = call(HttpMethod.PUT, "/api/v1/teacher/lessons/" + created.getBody().get("id") + "/level",
                open);
        assertThat(opened.getBody()).containsEntry("levelId", null);
    }

    private Student aStudentOn(String level) {
        Student student = aStudentWhoMayBook();
        record(student, level, null, null);
        return student;
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> record(Student student, String levelId, String goals, String privateNotes) {
        Map<String, Object> body = new HashMap<>();
        body.put("levelId", levelId);
        body.put("goals", goals);
        body.put("privateNotes", privateNotes);
        return call(HttpMethod.PUT, "/api/v1/teacher/students/" + student.id() + "/record", body);
    }

    private void onLevel(UUID lesson, String levelId) {
        Map<String, Object> body = new HashMap<>();
        body.put("levelId", levelId);
        assertThat(call(HttpMethod.PUT, "/api/v1/teacher/lessons/" + lesson + "/level", body).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @SuppressWarnings("rawtypes")
    private void join(Student student, UUID lesson) {
        assertThat(rest.exchange("/api/v1/lessons/" + lesson + "/waiting-list", HttpMethod.POST,
                new HttpEntity<>(bearer(student.token())), Map.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private boolean hasSeat(Student student, UUID lesson) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM bookings WHERE lesson_id = ? AND student_user_id = ? "
                + "AND status = 'CONFIRMED'", Integer.class, lesson, student.id());
        return count != null && count == 1;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Map> levels() {
        return (List<Map>) rest.exchange("/api/v1/levels", HttpMethod.GET, new HttpEntity<>(bearer(teacherToken)),
                Map.class).getBody().get("levels");
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> call(HttpMethod method, String path, Map<String, Object> body) {
        return rest.exchange(path, method, new HttpEntity<>(body, jsonBearer(teacherToken)), Map.class);
    }
}
