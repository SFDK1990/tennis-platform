package com.tennisplatform.notification;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.lesson.domain.LessonType;
import com.tennisplatform.notification.application.port.out.Mail;
import com.tennisplatform.teacher.application.port.in.QueryLevels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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

/** The teacher's messages and each student's board (37-fase24-analisis-mensajes.md). */
@ExtendWith(OutputCaptureExtension.class)
class MessagesApiTest extends AbstractBookingTest {

    private static final Duration IN_A_WEEK = Duration.ofDays(7);
    private static final String BODY = "Mañana no hay clase por la lluvia.";

    @Autowired
    private QueryLevels queryLevels;

    @Autowired
    private RecordingMailer mailer;

    private String beginners;
    private String advanced;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theTeacherHasTheirLevels() {
        queryLevels.seedDefaults(teacherId);
        List<Map> levels = (List<Map>) rest.exchange("/api/v1/levels", HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class).getBody().get("levels");
        beginners = (String) levels.get(0).get("id");
        advanced = (String) levels.get(2).get("id");
    }

    // --- who it reaches: one test per audience (decisions 1 and 2) ---------------

    @Test
    @SuppressWarnings("rawtypes")
    void toEverybodyReachesEveryManagedStudentAndNobodyElse() {
        Student lucia = aStudentWhoMayBook();
        Student pablo = aStudentWhoMayBook();
        Student notManaged = aStudentWithAProfile();

        ResponseEntity<Map> sent = send(Map.of("audience", "ALL", "subject", "Aviso", "body", BODY));

        assertThat(sent.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(sent.getBody()).containsEntry("recipientCount", 2).containsEntry("audienceLabel", "Todos los alumnos");
        assertThat(board(lucia)).singleElement().satisfies(message -> {
            assertThat(message).containsEntry("subject", "Aviso").containsEntry("body", BODY);
        });
        assertThat(board(pablo)).hasSize(1);
        assertThat(board(notManaged)).isEmpty();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void toALevelLeavesOutOtherLevelsAndStudentsWithoutOne() {
        Student onIt = aStudentOn(beginners);
        Student onAnother = aStudentOn(advanced);
        Student withoutLevel = aStudentWhoMayBook();

        ResponseEntity<Map> sent = send(Map.of("audience", "LEVEL", "levelId", beginners, "subject", "Iniciación",
                "body", BODY));

        assertThat(sent.getBody()).containsEntry("recipientCount", 1).containsEntry("audienceLabel", "Nivel Iniciación");
        assertThat(board(onIt)).hasSize(1);
        assertThat(board(onAnother)).isEmpty();
        assertThat(board(withoutLevel)).isEmpty();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void toALessonReachesWhoHoldsASeatInIt() {
        UUID lesson = aLessonStartingIn(IN_A_WEEK, LessonType.GROUP, 4);
        Student seated = aStudentWhoMayBook();
        Student cancelled = aStudentWhoMayBook();
        Student elsewhere = aStudentWhoMayBook();
        book(seated.token(), lesson);
        UUID booking = UUID.fromString((String) book(cancelled.token(), lesson).getBody().get("id"));
        rest.exchange("/api/v1/bookings/" + booking + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(cancelled.token())), Map.class);

        ResponseEntity<Map> sent = send(Map.of("audience", "LESSON", "lessonId", lesson.toString(), "subject", "Pista",
                "body", BODY));

        assertThat(sent.getBody()).containsEntry("recipientCount", 1);
        assertThat((String) sent.getBody().get("audienceLabel")).startsWith("Clase del ");
        assertThat(board(seated)).hasSize(1);
        assertThat(board(cancelled)).isEmpty();
        assertThat(board(elsewhere)).isEmpty();
    }

    /** A seat in a lesson of the series that already happened is not a place in the group. */
    @Test
    @SuppressWarnings("rawtypes")
    void toASeriesReachesWhoHoldsASeatInALessonStillToCome() {
        UUID first = aLessonStartingIn(IN_A_WEEK, LessonType.GROUP, 4);
        UUID second = aLessonStartingIn(IN_A_WEEK.plusDays(7), LessonType.GROUP, 4);
        UUID past = aLessonStartingIn(IN_A_WEEK.plusDays(14), LessonType.GROUP, 4);
        UUID series = UUID.randomUUID();
        jdbc.update("UPDATE lessons SET series_id = ? WHERE id IN (?, ?, ?)", series, first, second, past);
        Student inTheFirst = aStudentWhoMayBook();
        Student inTheSecond = aStudentWhoMayBook();
        Student onlyBefore = aStudentWhoMayBook();
        book(inTheFirst.token(), first);
        book(inTheSecond.token(), second);
        book(onlyBefore.token(), past);
        jdbc.update("UPDATE lessons SET starts_at = starts_at - interval '60 days', ends_at = ends_at - interval '60 days' "
                + "WHERE id = ?", past);

        ResponseEntity<Map> sent = send(Map.of("audience", "SERIES", "lessonId", second.toString(), "subject", "Grupo",
                "body", BODY));

        assertThat(sent.getBody()).containsEntry("recipientCount", 2);
        assertThat((String) sent.getBody().get("audienceLabel")).startsWith("Grupo de los ");
        assertThat(board(inTheFirst)).hasSize(1);
        assertThat(board(inTheSecond)).hasSize(1);
        assertThat(board(onlyBefore)).isEmpty();

        UUID single = aLessonStartingIn(IN_A_WEEK.plusDays(30), LessonType.GROUP, 4);
        assertThat(send(Map.of("audience", "SERIES", "lessonId", single.toString(), "subject", "Grupo", "body", BODY))
                .getBody()).containsEntry("code", "LESSON_NOT_IN_SERIES");

        // A series whose every lesson has already happened has nobody left in its group.
        jdbc.update("UPDATE lessons SET series_id = ? WHERE id = ?", UUID.randomUUID(), past);
        assertThat(send(Map.of("audience", "SERIES", "lessonId", past.toString(), "subject", "Grupo", "body", BODY))
                .getBody()).containsEntry("code", "NO_RECIPIENTS");
    }

    /** Decision 1: the recipients are fixed when it is sent. */
    @Test
    void aStudentWhoJoinsTheLevelAfterwardsDoesNotSeeWhatWasSentBefore() {
        aStudentOn(beginners);
        Student later = aStudentWhoMayBook();
        send(Map.of("audience", "LEVEL", "levelId", beginners, "subject", "Antes", "body", BODY));

        record(later, beginners);

        assertThat(board(later)).isEmpty();
    }

    // --- the email (decision 4) -----------------------------------------------

    @Test
    void everyRecipientWithAVerifiedAddressGetsItByEmailAndTheOthersOnlyOnTheBoard() {
        Student verified = aStudentWhoMayBook();
        Student unverified = aStudentWhoMayBook();
        jdbc.update("UPDATE users SET email_verified_at = NULL WHERE id = ?", unverified.id());
        mailer.clear();

        send(Map.of("audience", "ALL", "subject", "Cambio de horario", "body", BODY));

        assertThat(mailer.to(verified.email())).singleElement().satisfies(mail -> {
            assertThat(mail.kind()).isEqualTo("message-student");
            assertThat(mail.subject()).isEqualTo("Cambio de horario");
            assertThat(mail.body()).startsWith("Hola, Lucia Prieto:").contains("Ana Serrano te ha escrito:")
                    .contains(BODY).contains("/messages");
        });
        assertThat(mailer.to(unverified.email())).isEmpty();
        assertThat(board(unverified)).hasSize(1);
        assertThat(mailer.sent()).extracting(Mail::kind).containsOnly("message-student");
    }

    // --- refusals ------------------------------------------------------------

    @Test
    @SuppressWarnings("rawtypes")
    void aMessageThatReachesNobodyOrPointsNowhereIsRefused() {
        ResponseEntity<Map> nobody = send(Map.of("audience", "LEVEL", "levelId", advanced, "subject", "Hola",
                "body", BODY));
        assertThat(nobody.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(nobody.getBody()).containsEntry("code", "NO_RECIPIENTS");

        assertThat(send(Map.of("audience", "LEVEL", "levelId", UUID.randomUUID().toString(), "subject", "Hola",
                "body", BODY)).getBody()).containsEntry("code", "LEVEL_NOT_FOUND");
        assertThat(send(Map.of("audience", "LESSON", "lessonId", UUID.randomUUID().toString(), "subject", "Hola",
                "body", BODY)).getBody()).containsEntry("code", "LESSON_NOT_FOUND");

        aStudentWhoMayBook();
        assertThat(send(Map.of("audience", "ALL", "subject", " ", "body", BODY)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(send(Map.of("audience", "ALL", "subject", "Hola", "body", "x".repeat(4001))).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(send(Map.of("audience", "ALL", "levelId", beginners, "subject", "Hola", "body", BODY))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(send(Map.of("audience", "SOMEBODY", "subject", "Hola", "body", BODY)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- the board (decision 5) and deleting ---------------------------------

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theTeacherSeesWhatTheySentNewestFirstAndDeletingTakesItOffEveryBoard() {
        Student student = aStudentWhoMayBook();
        String first = (String) send(Map.of("audience", "ALL", "subject", "Primero", "body", BODY)).getBody().get("id");
        send(Map.of("audience", "ALL", "subject", "Segundo", "body", BODY));

        List<Map> sent = (List<Map>) rest.exchange("/api/v1/teacher/messages", HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class).getBody().get("items");
        assertThat(sent).extracting(message -> message.get("subject")).containsExactly("Segundo", "Primero");
        assertThat(board(student)).extracting(message -> message.get("subject")).containsExactly("Segundo", "Primero");

        ResponseEntity<Map> deleted = rest.exchange("/api/v1/teacher/messages/" + first, HttpMethod.DELETE,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(board(student)).extracting(message -> message.get("subject")).containsExactly("Segundo");

        ResponseEntity<Map> again = rest.exchange("/api/v1/teacher/messages/" + first, HttpMethod.DELETE,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
        assertThat(again.getBody()).containsEntry("code", "MESSAGE_NOT_FOUND");
        assertThat(board(teacherToken)).isEmpty();
    }

    // --- personal data (decision 6) ------------------------------------------

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theExportCarriesWhatTheStudentReceivedAndDeletingTheAccountForgetsThem() {
        Student leaving = aStudentWhoMayBook();
        Student staying = aStudentWhoMayBook();
        send(Map.of("audience", "ALL", "subject", "Aviso", "body", BODY));

        ResponseEntity<Map> export = rest.exchange("/api/v1/me/export", HttpMethod.GET,
                new HttpEntity<>(bearer(leaving.token())), Map.class);
        assertThat((List<Map>) export.getBody().get("messages")).singleElement()
                .satisfies(message -> assertThat(message).containsEntry("body", BODY));

        rest.exchange("/api/v1/me/deletion", HttpMethod.POST,
                new HttpEntity<>(Map.of("password", PASSWORD), jsonBearer(leaving.token())), Map.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM message_recipients WHERE student_user_id = ?",
                Integer.class, leaving.id())).isZero();
        assertThat(board(staying)).hasSize(1);
        List<Map> sent = (List<Map>) rest.exchange("/api/v1/teacher/messages", HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class).getBody().get("items");
        assertThat(sent).singleElement().satisfies(message -> assertThat(message).containsEntry("recipientCount", 2));
    }

    @Test
    void theLogNamesTheMessageButNeverWhatItSays(CapturedOutput output) {
        aStudentWhoMayBook();

        send(Map.of("audience", "ALL", "subject", "Asunto privado", "body", BODY));

        assertThat(output.getAll()).contains("sent to 1 students").doesNotContain(BODY).doesNotContain("Asunto privado");
    }

    // --- helpers -------------------------------------------------------------

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> send(Map<String, Object> body) {
        return rest.exchange("/api/v1/teacher/messages", HttpMethod.POST, new HttpEntity<>(body, jsonBearer(teacherToken)),
                Map.class);
    }

    private List<Map<String, Object>> board(Student student) {
        return board(student.token());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Map<String, Object>> board(String token) {
        return (List<Map<String, Object>>) rest.exchange("/api/v1/messages", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), Map.class).getBody().get("items");
    }

    private Student aStudentOn(String levelId) {
        Student student = aStudentWhoMayBook();
        record(student, levelId);
        return student;
    }

    @SuppressWarnings("rawtypes")
    private void record(Student student, String levelId) {
        Map<String, Object> body = new HashMap<>();
        body.put("levelId", levelId);
        ResponseEntity<Map> saved = rest.exchange("/api/v1/teacher/students/" + student.id() + "/record",
                HttpMethod.PUT, new HttpEntity<>(body, jsonBearer(teacherToken)), Map.class);
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
