package com.tennisplatform.student;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.notification.RecordingMailer;
import com.tennisplatform.teacher.application.port.in.QueryLevels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The objectives of each level and the ones each student achieved (44-fase29-analisis-progreso.md). */
@SuppressWarnings({"rawtypes", "unchecked"})
class ProgressApiTest extends AbstractBookingTest {

    @Autowired
    private QueryLevels queryLevels;

    @Autowired
    private RecordingMailer mailer;

    private String beginners;
    private String intermediate;

    @BeforeEach
    void theTeacherStartsWithTheFourLevels() {
        queryLevels.seedDefaults(teacherId);
        List<Map> levels = levels();
        beginners = (String) levels.get(0).get("id");
        intermediate = (String) levels.get(1).get("id");
    }

    @Test
    void theTeacherWritesRenamesReordersAndRetiresTheObjectivesOfALevel() {
        String rally = objective(beginners, "Peloteo de diez golpes");
        String serve = objective(beginners, "Saque por arriba");
        String volley = objective(beginners, "Volea de derecha");

        ResponseEntity<Map> repeated = teacher(HttpMethod.POST, "/api/v1/teacher/levels/" + beginners + "/objectives",
                Map.of("title", "Saque por arriba"));
        assertThat(repeated.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repeated.getBody()).containsEntry("code", "OBJECTIVE_ALREADY_EXISTS");
        assertThat(objective(intermediate, "Saque por arriba")).isNotNull();

        teacher(HttpMethod.PATCH, "/api/v1/teacher/objectives/" + volley, Map.of("title", "Volea", "position", 1));
        assertThat(objectivesOf(0)).extracting(o -> o.get("title"))
                .containsExactly("Volea", "Peloteo de diez golpes", "Saque por arriba");
        assertThat(objectivesOf(0)).extracting(o -> o.get("position")).containsExactly(1, 2, 3);

        assertThat(teacher(HttpMethod.DELETE, "/api/v1/teacher/objectives/" + rally, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(objectivesOf(0)).extracting(o -> o.get("title")).containsExactly("Volea", "Saque por arriba");
        assertThat(objectivesOf(0)).extracting(o -> o.get("position")).containsExactly(1, 2);
        assertThat(teacher(HttpMethod.PATCH, "/api/v1/teacher/objectives/" + rally, Map.of("title", "Otro"))
                .getBody()).containsEntry("code", "OBJECTIVE_NOT_FOUND");
        // A retired title is free for a new objective.
        assertThat(objective(beginners, "Peloteo de diez golpes")).isNotEqualTo(rally);
        assertThat(serve).isNotNull();
    }

    @Test
    void anObjectiveOfAnotherLevelIdIsNotFoundAndAnEmptyTitleIsRefused() {
        assertThat(teacher(HttpMethod.POST, "/api/v1/teacher/levels/" + UUID.randomUUID() + "/objectives",
                Map.of("title", "Algo")).getBody()).containsEntry("code", "LEVEL_NOT_FOUND");
        assertThat(teacher(HttpMethod.POST, "/api/v1/teacher/levels/" + beginners + "/objectives",
                Map.of("title", " ")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(teacher(HttpMethod.DELETE, "/api/v1/teacher/objectives/" + UUID.randomUUID(), null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void theTeacherMarksAnObjectiveTodayChangesItsDayAndUnmarksIt() {
        Student student = aStudentOn(beginners);
        String serve = objective(beginners, "Saque por arriba");
        String today = LocalDate.now(clock.withZone(ZoneId.of("Europe/Madrid"))).toString();

        ResponseEntity<Map> marked = mark(student, serve, null);
        assertThat(marked.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(achievedOn(progressOf(student), serve)).isEqualTo(today);

        String lastWeek = LocalDate.parse(today).minusDays(7).toString();
        mark(student, serve, lastWeek);
        assertThat(achievedOn(progressOf(student), serve)).isEqualTo(lastWeek);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM student_objectives WHERE student_user_id = ?",
                Integer.class, student.id())).isEqualTo(1);

        assertThat(teacher(HttpMethod.DELETE, "/api/v1/teacher/students/" + student.id() + "/objectives/" + serve, null)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(achievedOn(progressOf(student), serve)).isNull();
        assertThat(teacher(HttpMethod.DELETE, "/api/v1/teacher/students/" + student.id() + "/objectives/" + serve, null)
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void aDayInTheFutureIsRefused() {
        Student student = aStudentOn(beginners);
        String serve = objective(beginners, "Saque por arriba");
        String tomorrow = LocalDate.now(clock.withZone(ZoneId.of("Europe/Madrid"))).plusDays(1).toString();

        ResponseEntity<Map> refused = mark(student, serve, tomorrow);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(refused.getBody()).containsEntry("code", "ACHIEVED_ON_IN_FUTURE");
    }

    @Test
    void onlyALiveObjectiveOfTheTeachersLevelsIsMarkedAndOneOfAnotherLevelIsToo() {
        Student student = aStudentOn(beginners);
        String ahead = objective(intermediate, "Revés liftado");
        String retired = objective(beginners, "Retirado");
        teacher(HttpMethod.DELETE, "/api/v1/teacher/objectives/" + retired, null);

        assertThat(mark(student, ahead, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mark(student, retired, null).getBody()).containsEntry("code", "OBJECTIVE_NOT_FOUND");
        assertThat(mark(student, UUID.randomUUID().toString(), null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(teacher(HttpMethod.DELETE, "/api/v1/teacher/students/" + student.id() + "/objectives/"
                + UUID.randomUUID(), null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /** Mutation: letting a teacher mark a student they do not manage fails here. */
    @Test
    void onlyTheTeacherWhoManagesTheStudentMarksAndReads() {
        String serve = objective(beginners, "Saque por arriba");
        Student stranger = aStudentWithAProfile();

        ResponseEntity<Map> refused = mark(stranger, serve, null);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refused.getBody()).containsEntry("code", "STUDENT_NOT_MANAGED");
        assertThat(teacher(HttpMethod.GET, "/api/v1/teacher/students/" + stranger.id() + "/progress", null)
                .getBody()).containsEntry("code", "STUDENT_NOT_MANAGED");
        assertThat(rest.exchange("/api/v1/teacher/students/" + stranger.id() + "/objectives/" + serve,
                HttpMethod.PUT, new HttpEntity<>(bearer(stranger.token())), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM student_objectives", Integer.class)).isZero();
    }

    @Test
    void theRecordShowsEveryLevelAndTheStudentTheirsAndThoseWhereTheyAchievedSomething() {
        Student student = aStudentOn(intermediate);
        String rally = objective(beginners, "Peloteo de diez golpes");
        objective(beginners, "Saque por arriba");
        String backhand = objective(intermediate, "Revés liftado");
        mark(student, rally, null);

        List<Map> record = (List<Map>) progressOf(student).get("levels");
        assertThat(record).extracting(level -> level.get("levelName"))
                .containsExactly("Iniciación", "Intermedio", "Avanzado", "Competición");

        List<Map> mine = (List<Map>) myProgress(student).get("levels");
        assertThat(mine).extracting(level -> level.get("levelName")).containsExactly("Iniciación", "Intermedio");
        assertThat(mine.get(1)).containsEntry("current", true).containsEntry("completed", false);
        assertThat((List<Map>) mine.get(1).get("objectives")).singleElement()
                .satisfies(objective -> assertThat(objective).containsEntry("id", backhand)
                        .containsEntry("achievedOn", null));
        assertThat(mine.get(0)).containsEntry("current", false);
    }

    /** Mutation: showing the student the retired objectives they did not achieve fails here. */
    @Test
    void aRetiredObjectiveStaysForWhoeverAchievedItAndDisappearsForTheRest() {
        Student achiever = aStudentOn(beginners);
        Student other = aStudentOn(beginners);
        String rally = objective(beginners, "Peloteo de diez golpes");
        String serve = objective(beginners, "Saque por arriba");
        mark(achiever, rally, null);

        teacher(HttpMethod.DELETE, "/api/v1/teacher/objectives/" + rally, null);

        assertThat(objectivesIn(myProgress(achiever), 0)).extracting(o -> o.get("id")).containsExactly(serve, rally);
        assertThat(objectivesIn(myProgress(other), 0)).extracting(o -> o.get("id")).containsExactly(serve);
    }

    @Test
    void aStudentWithoutLevelOrWithoutObjectivesSeesSo() {
        Student withoutLevel = aStudentWhoMayBook();
        assertThat((List<Map>) myProgress(withoutLevel).get("levels")).isEmpty();

        Student onAnEmptyLevel = aStudentOn(intermediate);
        assertThat((List<Map>) myProgress(onAnEmptyLevel).get("levels")).singleElement()
                .satisfies(level -> assertThat(level).containsEntry("current", true).containsEntry("completed", false)
                        .containsEntry("objectives", List.of()));

        Student notManaged = aStudentWithAProfile();
        assertThat((List<Map>) myProgress(notManaged).get("levels")).isEmpty();
    }

    @Test
    void completingTheLevelShowsInTheListAndANewObjectiveTakesItAway() {
        Student student = aStudentOn(beginners);
        Student behind = aStudentOn(beginners);
        String rally = objective(beginners, "Peloteo de diez golpes");
        String serve = objective(beginners, "Saque por arriba");
        mark(student, rally, null);
        mark(student, serve, null);
        mark(behind, rally, null);

        assertThat(levelCompleted(student)).isTrue();
        assertThat(levelCompleted(behind)).isFalse();
        assertThat(((List<Map>) progressOf(student).get("levels")).get(0)).containsEntry("completed", true);

        objective(beginners, "Volea");
        assertThat(levelCompleted(student)).isFalse();
    }

    @Test
    void theStudentGetsANoticeOnTheBoardTheFirstTimeAndNoEmail() {
        Student student = aStudentOn(beginners);
        String serve = objective(beginners, "Saque por arriba");
        mailer.clear();

        mark(student, serve, null);
        mark(student, serve, LocalDate.now(clock.withZone(ZoneId.of("Europe/Madrid"))).minusDays(1).toString());

        List<Map> board = (List<Map>) rest.exchange("/api/v1/messages", HttpMethod.GET,
                new HttpEntity<>(bearer(student.token())), Map.class).getBody().get("items");
        assertThat(board).singleElement().satisfies(notice -> {
            assertThat((String) notice.get("subject")).contains("Saque por arriba");
            assertThat((String) notice.get("body")).contains("Ana Serrano").contains("Iniciación");
        });
        assertThat(mailer.sent()).isEmpty();
        assertThat((List<Map>) teacher(HttpMethod.GET, "/api/v1/teacher/messages", null).getBody().get("items"))
                .isEmpty();

        teacher(HttpMethod.DELETE, "/api/v1/teacher/students/" + student.id() + "/objectives/" + serve, null);
        assertThat((List<Map>) rest.exchange("/api/v1/messages", HttpMethod.GET,
                new HttpEntity<>(bearer(student.token())), Map.class).getBody().get("items")).hasSize(1);
    }

    @Test
    void theTeacherCannotSendAProgressNoticeAsAMessage() {
        aStudentOn(beginners);

        ResponseEntity<Map> refused = teacher(HttpMethod.POST, "/api/v1/teacher/messages",
                Map.of("audience", "PROGRESS", "subject", "Hola", "body", "Texto"));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aLevelWithAnAchievedObjectiveIsInUseAndOneWithUnachievedObjectivesIsDeleted() {
        Student student = aStudentOn(beginners);
        String rally = objective(intermediate, "Revés liftado");
        mark(student, rally, null);
        String advanced = (String) levels().get(2).get("id");
        objective(advanced, "Dejada");

        assertThat(teacher(HttpMethod.DELETE, "/api/v1/teacher/levels/" + intermediate, null).getBody())
                .containsEntry("code", "LEVEL_IN_USE");
        assertThat(teacher(HttpMethod.DELETE, "/api/v1/teacher/levels/" + advanced, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM level_objectives WHERE level_id = ?::uuid",
                Integer.class, advanced)).isZero();
    }

    /** Mutation: deleting an achieved objective instead of retiring it fails here, in the schema itself. */
    @Test
    void theDatabaseRefusesToDeleteAnAchievedObjective() {
        Student student = aStudentOn(beginners);
        String rally = objective(beginners, "Peloteo de diez golpes");
        mark(student, rally, null);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM level_objectives WHERE id = ?::uuid", rally))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT retired_at IS NULL FROM level_objectives WHERE id = ?::uuid",
                Boolean.class, rally)).isTrue();
    }

    @Test
    void lettingTheStudentGoKeepsTheirProgressAndTheExportCarriesIt() {
        Student student = aStudentOn(beginners);
        String rally = objective(beginners, "Peloteo de diez golpes");
        mark(student, rally, null);

        teacher(HttpMethod.DELETE, "/api/v1/teacher/students/" + student.id() + "/manage", null);
        assertThat(achievedOn(progressOf(student), rally)).isNotNull();
        assertThat(mark(student, rally, null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        manage(student);

        Map export = rest.exchange("/api/v1/me/export", HttpMethod.GET, new HttpEntity<>(bearer(student.token())),
                Map.class).getBody();
        assertThat((List<Map>) export.get("progress")).singleElement()
                .satisfies(level -> assertThat((List<Map>) level.get("objectives")).singleElement()
                        .satisfies(objective -> assertThat(objective).containsEntry("title", "Peloteo de diez golpes")
                                .doesNotContainEntry("achievedOn", null)));
    }

    @Test
    void theTeacherCannotReadAStudentsOwnProgress() {
        assertThat(rest.exchange("/api/v1/me/progress", HttpMethod.GET, new HttpEntity<>(bearer(teacherToken)),
                Map.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Student aStudentOn(String levelId) {
        Student student = aStudentWhoMayBook();
        Map<String, Object> body = new HashMap<>();
        body.put("levelId", levelId);
        assertThat(teacher(HttpMethod.PUT, "/api/v1/teacher/students/" + student.id() + "/record", body)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        return student;
    }

    private String objective(String levelId, String title) {
        ResponseEntity<Map> created = teacher(HttpMethod.POST, "/api/v1/teacher/levels/" + levelId + "/objectives",
                Map.of("title", title));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) created.getBody().get("id");
    }

    private ResponseEntity<Map> mark(Student student, String objectiveId, String achievedOn) {
        Map<String, Object> body = achievedOn == null ? null : Map.of("achievedOn", achievedOn);
        return teacher(HttpMethod.PUT, "/api/v1/teacher/students/" + student.id() + "/objectives/" + objectiveId, body);
    }

    private Map progressOf(Student student) {
        ResponseEntity<Map> progress = teacher(HttpMethod.GET, "/api/v1/teacher/students/" + student.id() + "/progress",
                null);
        assertThat(progress.getStatusCode()).isEqualTo(HttpStatus.OK);
        return progress.getBody();
    }

    private Map myProgress(Student student) {
        ResponseEntity<Map> progress = rest.exchange("/api/v1/me/progress", HttpMethod.GET,
                new HttpEntity<>(bearer(student.token())), Map.class);
        assertThat(progress.getStatusCode()).isEqualTo(HttpStatus.OK);
        return progress.getBody();
    }

    private static List<Map> objectivesIn(Map progress, int level) {
        return (List<Map>) ((List<Map>) progress.get("levels")).get(level).get("objectives");
    }

    private static Object achievedOn(Map progress, String objectiveId) {
        return ((List<Map>) progress.get("levels")).stream()
                .flatMap(level -> ((List<Map>) level.get("objectives")).stream())
                .filter(objective -> objectiveId.equals(objective.get("id")))
                .findFirst()
                .orElseThrow()
                .get("achievedOn");
    }

    private boolean levelCompleted(Student student) {
        List<Map> items = (List<Map>) teacher(HttpMethod.GET, "/api/v1/teacher/students", null).getBody().get("items");
        return items.stream().filter(item -> student.id().toString().equals(item.get("userId")))
                .map(item -> (Boolean) item.get("levelCompleted"))
                .findFirst()
                .orElseThrow();
    }

    private List<Map> levels() {
        return (List<Map>) rest.exchange("/api/v1/levels", HttpMethod.GET, new HttpEntity<>(bearer(teacherToken)),
                Map.class).getBody().get("levels");
    }

    private List<Map> objectivesOf(int level) {
        return (List<Map>) levels().get(level).get("objectives");
    }

    private ResponseEntity<Map> teacher(HttpMethod method, String path, Map<String, Object> body) {
        return rest.exchange(path, method, new HttpEntity<>(body, jsonBearer(teacherToken)), Map.class);
    }
}
