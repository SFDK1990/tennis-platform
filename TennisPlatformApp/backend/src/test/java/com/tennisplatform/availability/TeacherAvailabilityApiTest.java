package com.tennisplatform.availability;

import com.tennisplatform.AbstractIntegrationTest;
import com.tennisplatform.availability.application.port.in.QueryAvailability;
import com.tennisplatform.availability.application.port.out.AvailabilityRuleRepository;
import com.tennisplatform.identity.application.port.in.ProvisionTeacherAccount;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The availability endpoints over real HTTP, against a real PostgreSQL.
 *
 * <p>The resolution rules themselves are covered by the domain tests, which cost milliseconds.
 * What only this level can show is the part that lives outside the domain: who is allowed to
 * call what, that the migration produced a schema the entities actually fit, and that replacing
 * the weekly set really replaces it in the database.
 */
class TeacherAvailabilityApiTest extends AbstractIntegrationTest {

    private static final String TEACHER_EMAIL = "teacher@tennis-platform.local";
    private static final String PASSWORD = "a-valid-password";
    private static final String AVAILABILITY = "/api/v1/teacher/availability";

    @Autowired
    private ProvisionTeacherAccount accounts;

    @Autowired
    private TeacherProfileRepository profiles;

    @Autowired
    private AvailabilityRuleRepository rules;

    @Autowired
    private QueryAvailability queryAvailability;

    @Autowired
    private Clock clock;

    private UUID teacherId;

    /** Every test here needs a teacher: the tables have a mandatory foreign key to their profile. */
    @BeforeEach
    void seedTheTeacher() {
        teacherId = accounts.provision(TEACHER_EMAIL, PASSWORD);
        profiles.save(TeacherProfile.create(teacherId, "Ana Serrano", null, "Europe/Madrid",
                clock.instant()));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherConfiguresAWeekAndReadsItBack() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);

        ResponseEntity<Map> put = putWeekly(token, List.of(
                weeklyRule("MONDAY", "09:00", "13:00"),
                weeklyRule("MONDAY", "16:00", "20:00"),
                weeklyRule("WEDNESDAY", "09:00", "13:00")));

        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) put.getBody().get("rules")).hasSize(3);

        ResponseEntity<Map> get = get(token, "2026-01-01", "2026-01-31");
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) get.getBody().get("weeklyRules")).hasSize(3);
        assertThat((List<?>) get.getBody().get("exceptions")).isEmpty();
    }

    /**
     * The endpoint replaces rather than merges, and the old rows really have to be gone. Checking
     * through the repository as well as the response is what distinguishes "replaced" from
     * "answered as if it had replaced".
     */
    @Test
    void replacingTheWeeklySetRemovesWhatWasThereBefore() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        putWeekly(token, List.of(weeklyRule("MONDAY", "09:00", "13:00"),
                weeklyRule("TUESDAY", "09:00", "13:00")));

        putWeekly(token, List.of(weeklyRule("FRIDAY", "17:00", "19:00")));

        assertThat(rules.findByTeacher(teacherId)).hasSize(1);
        assertThat(rules.findByTeacher(teacherId).get(0).dayOfWeek().name()).isEqualTo("FRIDAY");
    }

    /** Criterion 4, over HTTP: rejected, and the previous configuration survives untouched. */
    @Test
    @SuppressWarnings("rawtypes")
    void overlappingRulesAreRejectedAndTheOldConfigurationSurvives() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        putWeekly(token, List.of(weeklyRule("MONDAY", "09:00", "13:00")));

        ResponseEntity<Map> response = putWeekly(token, List.of(
                weeklyRule("TUESDAY", "09:00", "13:00"),
                weeklyRule("TUESDAY", "12:00", "15:00")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "AVAILABILITY_RULES_OVERLAP");
        assertThat(rules.findByTeacher(teacherId)).hasSize(1);
    }

    /** Criterion 5, over HTTP. */
    @Test
    void adjacentRulesAreAccepted() {
        ResponseEntity<Map> response = putWeekly(tokenOf(TEACHER_EMAIL, PASSWORD), List.of(
                weeklyRule("MONDAY", "09:00", "11:00"),
                weeklyRule("MONDAY", "11:00", "13:00")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /** Criterion 8: a student reads the schedule, and cannot touch it. */
    @Test
    @SuppressWarnings("rawtypes")
    void aStudentCanReadTheAvailabilityButNotChangeIt() {
        putWeekly(tokenOf(TEACHER_EMAIL, PASSWORD), List.of(weeklyRule("MONDAY", "09:00", "13:00")));
        String studentToken = tokenOfANewStudent(PASSWORD);

        ResponseEntity<Map> read = get(studentToken, "2026-01-01", "2026-01-31");
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) read.getBody().get("weeklyRules")).hasSize(1);

        ResponseEntity<Map> write = putWeekly(studentToken,
                List.of(weeklyRule("SUNDAY", "09:00", "13:00")));
        assertThat(write.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(write.getBody()).containsEntry("code", "TEACHER_FORBIDDEN");
        assertThat(rules.findByTeacher(teacherId)).hasSize(1);
    }

    @Test
    void anAnonymousCallerCannotEvenRead() {
        assertThat(rest.getForEntity(AVAILABILITY + "?from=2026-01-01&to=2026-01-31", String.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** Criterion 10. */
    @Test
    @SuppressWarnings("rawtypes")
    void aRangeWiderThanTheCapIsRejected() {
        ResponseEntity<Map> response = get(tokenOf(TEACHER_EMAIL, PASSWORD), "2026-01-01", "2026-12-31");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "DATE_RANGE_INVALID");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void exceptionsAreCreatedListedAndDeleted() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        putWeekly(token, List.of(weeklyRule("MONDAY", "09:00", "13:00")));

        ResponseEntity<Map> created = postException(token,
                Map.of("date", "2026-01-05", "type", "BLOCK"));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).containsEntry("type", "BLOCK");
        assertThat(created.getBody().get("startTime")).isNull();

        ResponseEntity<Map> listed = get(token, "2026-01-01", "2026-01-31");
        assertThat((List<?>) listed.getBody().get("exceptions")).hasSize(1);

        String id = (String) created.getBody().get("id");
        ResponseEntity<String> deleted = rest.exchange(AVAILABILITY + "/exceptions/" + id,
                HttpMethod.DELETE, new HttpEntity<>(bearer(token)), String.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> afterDelete = get(token, "2026-01-01", "2026-01-31");
        assertThat((List<?>) afterDelete.getBody().get("exceptions")).isEmpty();
    }

    /** Outside the requested range an exception is simply not part of the answer. */
    @Test
    @SuppressWarnings("rawtypes")
    void anExceptionOutsideTheRangeIsNotReturned() {
        String token = tokenOf(TEACHER_EMAIL, PASSWORD);
        postException(token, Map.of("date", "2026-03-10", "type", "BLOCK"));

        ResponseEntity<Map> response = get(token, "2026-01-01", "2026-01-31");

        assertThat((List<?>) response.getBody().get("exceptions")).isEmpty();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void deletingSomethingThatIsNotThereIsANotFound() {
        ResponseEntity<Map> response = rest.exchange(
                AVAILABILITY + "/exceptions/" + UUID.randomUUID(), HttpMethod.DELETE,
                new HttpEntity<>(bearer(tokenOf(TEACHER_EMAIL, PASSWORD))), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("code", "AVAILABILITY_EXCEPTION_NOT_FOUND");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aWeekdayGivenAsANumberIsRejected() {
        ResponseEntity<Map> response = putWeekly(tokenOf(TEACHER_EMAIL, PASSWORD),
                List.of(Map.of("dayOfWeek", "0", "startTime", "09:00", "endTime", "13:00")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "AVAILABILITY_INVALID");
    }

    /**
     * The port the later phases will consume, end to end: rules written over HTTP come back as
     * resolved instants. A Monday of January in Madrid is UTC+1, so 09:00 local is 08:00Z.
     */
    @Test
    void theQueryPortResolvesWhatWasConfiguredOverHttp() {
        putWeekly(tokenOf(TEACHER_EMAIL, PASSWORD), List.of(weeklyRule("MONDAY", "09:00", "13:00")));

        assertThat(queryAvailability.covers(teacherId, Instant.parse("2026-01-05T09:00:00Z"),
                Instant.parse("2026-01-05T10:00:00Z"))).isTrue();
        assertThat(queryAvailability.covers(teacherId, Instant.parse("2026-01-05T12:30:00Z"),
                Instant.parse("2026-01-05T13:00:00Z"))).isFalse();
        assertThat(queryAvailability.intervals(teacherId, Instant.parse("2026-01-05T00:00:00Z"),
                Instant.parse("2026-01-05T23:00:00Z"))).hasSize(1);
    }

    // --- helpers ---------------------------------------------------------------

    private static Map<String, String> weeklyRule(String day, String start, String end) {
        return Map.of("dayOfWeek", day, "startTime", start, "endTime", end);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> putWeekly(String token, List<? extends Map<String, String>> rules) {
        return rest.exchange(AVAILABILITY + "/weekly", HttpMethod.PUT,
                new HttpEntity<>(Map.of("rules", rules), jsonBearer(token)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> postException(String token, Map<String, String> body) {
        return rest.exchange(AVAILABILITY + "/exceptions", HttpMethod.POST,
                new HttpEntity<>(body, jsonBearer(token)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> get(String token, String from, String to) {
        return rest.exchange(AVAILABILITY + "?from=" + from + "&to=" + to, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), Map.class);
    }
}
