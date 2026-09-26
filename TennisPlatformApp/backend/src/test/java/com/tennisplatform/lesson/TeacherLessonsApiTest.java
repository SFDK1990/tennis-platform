package com.tennisplatform.lesson;

import com.tennisplatform.AbstractIntegrationTest;
import com.tennisplatform.identity.application.port.in.ProvisionTeacherAccount;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonOverlapException;
import com.tennisplatform.lesson.domain.LessonPeriod;
import com.tennisplatform.lesson.domain.LessonType;
import com.tennisplatform.teacher.application.port.out.TeacherProfileRepository;
import com.tennisplatform.teacher.domain.TeacherProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The lesson endpoints over real HTTP, against a real PostgreSQL.
 *
 * <p>The rules themselves - duration, midnight, capacity - are covered by the domain tests,
 * which cost milliseconds. What only this level can show is the part that lives outside the
 * domain: that the migration produced a schema the entities fit, that the exclusion constraint
 * exists and is translated instead of surfacing as a 500, who is allowed to call what, and that
 * a status nobody stores is still the right one when it is read back.
 *
 * <p>Dates are derived from the clock rather than written as literals, because a lesson in the
 * past is a different lesson: a fixed date would quietly start testing the COMPLETED path the
 * year it went by.
 */
class TeacherLessonsApiTest extends AbstractIntegrationTest {

    private static final String TEACHER_EMAIL = "teacher@tennis-platform.local";
    private static final String PASSWORD = "a-valid-password";
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    private static final String LESSONS = "/api/v1/teacher/lessons";
    private static final String AVAILABILITY = "/api/v1/teacher/availability/weekly";

    @Autowired
    private ProvisionTeacherAccount accounts;

    @Autowired
    private TeacherProfileRepository profiles;

    @Autowired
    private LessonRepository lessons;

    @Autowired
    private Clock clock;

    private UUID teacherId;
    private LocalDate workingDay;

    /** Every lesson needs a teacher: the table has a mandatory foreign key to their profile. */
    @BeforeEach
    void seedTheTeacher() {
        teacherId = accounts.provision(TEACHER_EMAIL, PASSWORD);
        profiles.save(TeacherProfile.create(teacherId, "Ana Serrano", null, "Europe/Madrid",
                clock.instant()));
        workingDay = LocalDate.now(clock.withZone(MADRID)).plusDays(30);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherSchedulesALessonInsideTheirOwnHours() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        openTheWorkingDay(token);

        ResponseEntity<Map> created = create(token, lesson("INDIVIDUAL", 1, 10, 11, false));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("status")).isEqualTo("OPEN");
        assertThat(created.getBody().get("createdOutsideAvailability")).isEqualTo(false);
        assertThat(created.getBody().get("id")).isNotNull();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void refusesALessonOutsideTheHoursAndAcceptsItWhenAskedForOnPurpose() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        openTheWorkingDay(token);

        ResponseEntity<Map> refused = create(token, lesson("INDIVIDUAL", 1, 20, 21, false));
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(refused.getBody().get("code")).isEqualTo("LESSON_OUTSIDE_AVAILABILITY");

        ResponseEntity<Map> forced = create(token, lesson("INDIVIDUAL", 1, 20, 21, true));
        assertThat(forced.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(forced.getBody().get("createdOutsideAvailability")).isEqualTo(true);
    }

    /**
     * The application checks for an overlap before writing, so this is the answer that check
     * produces. That the database would also refuse it is what the next test shows.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void refusesALessonThatOverlapsAnotherOne() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        openTheWorkingDay(token);
        create(token, lesson("INDIVIDUAL", 1, 10, 11, false));

        ResponseEntity<Map> overlapping = create(token, lesson("GROUP", 4, 10, 12, false));

        assertThat(overlapping.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(overlapping.getBody().get("code")).isEqualTo("LESSON_OVERLAP");
    }

    /** Two lessons that merely touch are not an overlap, and the constraint has to agree. */
    @Test
    @SuppressWarnings("rawtypes")
    void acceptsALessonThatStartsExactlyWhenAnotherEnds() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        openTheWorkingDay(token);
        create(token, lesson("INDIVIDUAL", 1, 10, 11, false));

        ResponseEntity<Map> next = create(token, lesson("INDIVIDUAL", 1, 11, 12, false));

        assertThat(next.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    /**
     * Cancelling frees the slot, which is what the {@code WHERE (status <> 'CANCELLED')} clause
     * of the exclusion constraint is for - and it is exactly what a teacher does next.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void theSlotOfACancelledLessonCanBeUsedAgain() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        openTheWorkingDay(token);
        String id = (String) create(token, lesson("INDIVIDUAL", 1, 10, 11, false)).getBody().get("id");

        ResponseEntity<Map> cancelled = cancel(token, id);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody().get("status")).isEqualTo("CANCELLED");
        assertThat(cancelled.getBody().get("cancelledAt")).isNotNull();
        assertThat(cancelled.getBody().get("cancelledAtShortNotice")).isEqualTo(false);

        assertThat(create(token, lesson("GROUP", 3, 10, 11, false)).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void refusesToCancelTheSameLessonTwice() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        openTheWorkingDay(token);
        String id = (String) create(token, lesson("INDIVIDUAL", 1, 10, 11, false)).getBody().get("id");
        cancel(token, id);

        ResponseEntity<Map> again = cancel(token, id);

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(again.getBody().get("code")).isEqualTo("LESSON_ALREADY_CANCELLED");
    }

    /**
     * Nothing ran and nothing was written, yet the lesson reads COMPLETED: the status follows
     * from the clock at the moment it is read. This is the whole point of not storing it.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void aLessonThatHasAlreadyEndedReadsAsCompletedWithoutAnythingHavingRun() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        String id = aLessonLastMonth();

        ResponseEntity<Map> read = rest.exchange("/api/v1/lessons/" + id, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), Map.class);

        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read.getBody().get("status")).isEqualTo("COMPLETED");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void refusesToCancelALessonThatAlreadyEnded() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        String id = aLessonLastMonth();

        ResponseEntity<Map> refused = cancel(token, id);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(refused.getBody().get("code")).isEqualTo("LESSON_ALREADY_FINISHED");
    }

    /** Decision 6 of 20-fase9-analisis-booking.md: a lesson that has already started has no use. */
    @Test
    @SuppressWarnings("rawtypes")
    void refusesALessonThatWouldAlreadyHaveStarted() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        LocalDate lastMonth = LocalDate.now(clock.withZone(MADRID)).minusDays(30);

        ResponseEntity<Map> refused = create(token, lessonOn(lastMonth, "INDIVIDUAL", 1, 10, 11, true));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(refused.getBody().get("code")).isEqualTo("LESSON_IN_THE_PAST");
    }

    /**
     * The API no longer creates a lesson in the past, so one is written straight through the
     * repository. That is the only way such a lesson exists now: it was created before and time
     * went by.
     */
    private String aLessonLastMonth() {
        return lessons.save(lesson(LocalDate.now(clock.withZone(MADRID)).minusDays(30), 10, 11))
                .id().toString();
    }

    /** The notes are the teacher's own shorthand; they were not written for the student to read. */
    @Test
    @SuppressWarnings("rawtypes")
    void aStudentReadsTheLessonButNotTheTeachersNotes() {
        String teacherToken = tokenOf(TEACHER_EMAIL, PASSWORD);
        openTheWorkingDay(teacherToken);
        Map<String, Object> body = lesson("GROUP", 4, 10, 11, false);
        body.put("notes", "insistir con el revés");
        String id = (String) create(teacherToken, body).getBody().get("id");

        String studentToken = tokenOfANewStudent(PASSWORD);
        ResponseEntity<Map> asStudent = rest.exchange("/api/v1/lessons/" + id, HttpMethod.GET,
                new HttpEntity<>(bearer(studentToken)), Map.class);

        assertThat(asStudent.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(asStudent.getBody().get("capacity")).isEqualTo(4);
        assertThat(asStudent.getBody().get("notes")).isNull();

        ResponseEntity<Map> asTeacher = rest.exchange("/api/v1/lessons/" + id, HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
        assertThat(asTeacher.getBody().get("notes")).isEqualTo("insistir con el revés");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aStudentCannotScheduleALesson() {
        String studentToken = tokenOfANewStudent(PASSWORD);

        ResponseEntity<Map> refused = create(studentToken, lesson("INDIVIDUAL", 1, 10, 11, true));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(refused.getBody().get("code")).isEqualTo("TEACHER_FORBIDDEN");

        String teacherToken = tokenOf(TEACHER_EMAIL, PASSWORD);
        openTheWorkingDay(teacherToken);
        String id = (String) create(teacherToken, lesson("INDIVIDUAL", 1, 10, 11, false)).getBody().get("id");
        assertThat(cancel(studentToken, id).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * The database's own guarantee, exercised on its own.
     *
     * <p>Every other overlap test is answered by the check the service makes before writing, so
     * none of them proves the constraint exists - they would all still pass if it had never been
     * created. Going straight to the repository skips that check and leaves only the schema
     * between the two lessons, which is the arrangement a second request racing the first would
     * find. It also covers the translation of the violation, which is otherwise unreachable.
     */
    @Test
    void theDatabaseRefusesAnOverlapThatSkippedTheApplicationCheck() {
        LocalDate day = workingDay;
        lessons.save(lesson(day, 10, 11));

        assertThatThrownBy(() -> lessons.save(lesson(day, 10, 12)))
                .isInstanceOf(LessonOverlapException.class);
    }

    /** And it lets through the one the application would also accept, so the filter is not simply "always no". */
    @Test
    void theDatabaseAcceptsAdjacentLessonsThroughTheSamePath() {
        LocalDate day = workingDay;
        lessons.save(lesson(day, 10, 11));

        assertThatCode(() -> lessons.save(lesson(day, 11, 12))).doesNotThrowAnyException();
    }

    private Lesson lesson(LocalDate day, int fromHour, int toHour) {
        return Lesson.create(teacherId, LessonType.INDIVIDUAL,
                new LessonPeriod(at(day, LocalTime.of(fromHour, 0)), at(day, LocalTime.of(toHour, 0))),
                1, null, true, MADRID, 8);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void refusesADurationThatIsNotAWholeNumberOfHalfHours() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        Map<String, Object> body = lesson("INDIVIDUAL", 1, 10, 11, true);
        body.put("endsAt", at(workingDay, LocalTime.of(10, 45)).toString());

        ResponseEntity<Map> refused = create(token, body);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(refused.getBody().get("code")).isEqualTo("LESSON_INVALID");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void answersNotFoundForALessonThatDoesNotExist() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);

        ResponseEntity<Map> missing = rest.exchange("/api/v1/lessons/" + UUID.randomUUID(),
                HttpMethod.GET, new HttpEntity<>(bearer(token)), Map.class);

        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody().get("code")).isEqualTo("LESSON_NOT_FOUND");

        ResponseEntity<Map> cancelMissing = cancel(token, UUID.randomUUID().toString());
        assertThat(cancelMissing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(cancelMissing.getBody().get("code")).isEqualTo("LESSON_NOT_FOUND");
    }

    /**
     * A malformed id used to fall through to the catch-all handler and answer 500. It is the
     * caller's mistake, and the answer has to say so.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void aMalformedIdIsABadRequestNotAServerError() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);

        ResponseEntity<Map> response = rest.exchange("/api/v1/lessons/not-a-uuid", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("VALIDATION_ERROR");
    }

    /** Opens the day the lessons are scheduled on, through the availability API rather than its internals. */
    @SuppressWarnings("rawtypes")
    private void openTheWorkingDay(String token) {
        ResponseEntity<Map> response = rest.exchange(AVAILABILITY, HttpMethod.PUT,
                new HttpEntity<>(Map.of("rules", List.of(Map.of(
                        "dayOfWeek", workingDay.getDayOfWeek().name(),
                        "startTime", "09:00:00",
                        "endTime", "19:00:00"))), jsonBearer(token)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> create(String token, Map<String, Object> body) {
        return rest.exchange(LESSONS, HttpMethod.POST, new HttpEntity<>(body, jsonBearer(token)),
                Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> cancel(String token, String id) {
        return rest.exchange(LESSONS + "/" + id + "/cancel", HttpMethod.POST,
                new HttpEntity<>(jsonBearer(token)), Map.class);
    }

    private Map<String, Object> lesson(String type, int capacity, int fromHour, int toHour,
                                       boolean overrideAvailability) {
        return lessonOn(workingDay, type, capacity, fromHour, toHour, overrideAvailability);
    }

    private Map<String, Object> lessonOn(LocalDate day, String type, int capacity, int fromHour,
                                         int toHour, boolean overrideAvailability) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("type", type);
        body.put("startsAt", at(day, LocalTime.of(fromHour, 0)).toString());
        body.put("endsAt", at(day, LocalTime.of(toHour, 0)).toString());
        body.put("capacity", capacity);
        body.put("overrideAvailability", overrideAvailability);
        return body;
    }

    private static Instant at(LocalDate day, LocalTime time) {
        return day.atTime(time).atZone(MADRID).toInstant();
    }
}
