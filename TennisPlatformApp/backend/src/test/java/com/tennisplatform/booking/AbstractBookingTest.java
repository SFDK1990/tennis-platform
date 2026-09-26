package com.tennisplatform.booking;

import com.tennisplatform.AbstractIntegrationTest;
import com.tennisplatform.identity.application.port.in.ProvisionTeacherAccount;
import com.tennisplatform.lesson.application.port.out.LessonRepository;
import com.tennisplatform.lesson.domain.Lesson;
import com.tennisplatform.lesson.domain.LessonType;
import com.tennisplatform.teacher.application.port.out.TeacherProfileRepository;
import com.tennisplatform.teacher.domain.TeacherProfile;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What every booking test needs: a teacher, lessons at chosen instants, and students in each of
 * the states that matter - managed and verified, or missing one of the two.
 *
 * <p>Lessons are written through the repository with {@code Lesson.rehydrate}, not through the
 * API. The API now refuses a lesson in the past and checks availability and midnight, and a
 * booking test needs lessons that started an hour ago, or that start in five hours whatever the
 * time of day the suite runs at - neither of which it could get from the API reliably.
 */
abstract class AbstractBookingTest extends AbstractIntegrationTest {

    protected static final String TEACHER_EMAIL = "teacher@tennis-platform.local";
    protected static final String PASSWORD = "a-valid-password";

    @Autowired
    private ProvisionTeacherAccount accounts;

    @Autowired
    private TeacherProfileRepository teacherProfiles;

    @Autowired
    private LessonRepository lessons;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected Clock clock;

    protected UUID teacherId;
    protected String teacherToken;

    @BeforeEach
    void seedTheTeacher() {
        teacherId = accounts.provision(TEACHER_EMAIL, PASSWORD);
        teacherProfiles.save(TeacherProfile.create(teacherId, "Ana Serrano", null, "Europe/Madrid",
                clock.instant()));
        teacherToken = tokenOf(TEACHER_EMAIL, PASSWORD);
    }

    /** A lesson that starts {@code fromNow} after the current instant and lasts an hour. Lessons of one test must be at least an hour apart. */
    protected UUID aLessonStartingIn(Duration fromNow, LessonType type, int capacity) {
        Instant startsAt = clock.instant().plus(fromNow).truncatedTo(ChronoUnit.MINUTES);
        return lessons.save(Lesson.rehydrate(null, teacherId, type, startsAt,
                startsAt.plus(Duration.ofHours(1)), capacity, null, false, null)).id();
    }

    protected UUID anIndividualLessonIn(Duration fromNow) {
        return aLessonStartingIn(fromNow, LessonType.INDIVIDUAL, 1);
    }

    protected record Student(UUID id, String email, String token) {
    }

    /** Registered, with a profile, verified, and managed by the teacher: somebody who may book. */
    protected Student aStudentWhoMayBook() {
        Student student = aStudentWithAProfile();
        verify(student);
        manage(student);
        return new Student(student.id(), student.email(), tokenOf(student.email(), PASSWORD));
    }

    @SuppressWarnings("rawtypes")
    protected Student aStudentWithAProfile() {
        String email = "student-" + UUID.randomUUID() + "@example.com";
        rest.postForEntity("/api/v1/auth/register", Map.of("email", email, "password", PASSWORD),
                Map.class);
        String token = tokenOf(email, PASSWORD);

        ResponseEntity<Map> saved = rest.exchange("/api/v1/me", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("fullName", "Lucia Prieto"), jsonBearer(token)), Map.class);
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
        return new Student(UUID.fromString((String) saved.getBody().get("id")), email, token);
    }

    /**
     * Marks the address as verified straight in the table. The verification flow itself is
     * covered by the identity tests; what matters here is only that the next token says so,
     * which is why callers log in again afterwards.
     */
    protected void verify(Student student) {
        jdbc.update("UPDATE users SET email_verified_at = now(), status = 'ACTIVE' WHERE id = ?",
                student.id());
    }

    @SuppressWarnings("rawtypes")
    protected void manage(Student student) {
        ResponseEntity<Map> managed = rest.exchange("/api/v1/teacher/students/" + student.id() + "/manage",
                HttpMethod.POST, new HttpEntity<>(bearer(teacherToken)), Map.class);
        assertThat(managed.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> book(String token, UUID lessonId) {
        return rest.exchange("/api/v1/lessons/" + lessonId + "/bookings", HttpMethod.POST,
                new HttpEntity<>(bearer(token)), Map.class);
    }

    protected int confirmedBookingsOf(UUID lessonId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM bookings WHERE lesson_id = ? AND status = 'CONFIRMED'",
                Integer.class, lessonId);
        return count == null ? 0 : count;
    }
}
