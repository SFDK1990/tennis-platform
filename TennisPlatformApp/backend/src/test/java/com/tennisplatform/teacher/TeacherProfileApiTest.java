package com.tennisplatform.teacher;

import com.tennisplatform.AbstractIntegrationTest;
import com.tennisplatform.identity.application.port.in.ProvisionTeacherAccount;
import com.tennisplatform.teacher.application.port.out.TeacherProfileRepository;
import com.tennisplatform.teacher.domain.TeacherProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The teacher profile over real HTTP, which is where role and ownership are actually enforced.
 */
class TeacherProfileApiTest extends AbstractIntegrationTest {

    private static final String TEACHER_EMAIL = "teacher@tennis-platform.local";
    private static final String STUDENT_PASSWORD = "a-valid-password";

    @Autowired
    private ProvisionTeacherAccount accounts;

    @Autowired
    private TeacherProfileRepository profiles;

    @Autowired
    private Clock clock;

    private UUID teacherId;

    /**
     * Seeds the teacher through the public ports rather than with SQL, so the test exercises the
     * same path the bootstrap uses. The base class emptied the database first.
     */
    @BeforeEach
    void seedTheTeacher() {
        teacherId = accounts.provision(TEACHER_EMAIL, STUDENT_PASSWORD);
        profiles.save(TeacherProfile.create(teacherId, "Ana Serrano", null, "Europe/Madrid",
                clock.instant()));
    }

    @Test
    void anyAuthenticatedUserCanReadTheTeacherProfile() {
        String studentToken = tokenOfANewStudent(STUDENT_PASSWORD);

        ResponseEntity<Map> response = rest.exchange("/api/v1/teacher/profile", HttpMethod.GET,
                new HttpEntity<>(bearer(studentToken)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("displayName", "Ana Serrano");
        assertThat(response.getBody()).containsEntry("timezone", "Europe/Madrid");
    }

    @Test
    void anAnonymousCallerCannotReadIt() {
        assertThat(rest.getForEntity("/api/v1/teacher/profile", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void theTeacherCanUpdateTheirOwnProfile() {
        String teacherToken = tokenOf(TEACHER_EMAIL, STUDENT_PASSWORD);

        ResponseEntity<Map> response = patchProfile(teacherToken,
                Map.of("displayName", "Ana S.", "phone", "600123123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("displayName", "Ana S.");
        assertThat(response.getBody()).containsEntry("phone", "600123123");
        // Untouched fields survive a partial update.
        assertThat(response.getBody()).containsEntry("timezone", "Europe/Madrid");
    }

    /** Criterion of Fase 6: a student cannot edit the teacher, even with a valid token. */
    @Test
    void aStudentCannotUpdateTheTeacherProfile() {
        String studentToken = tokenOfANewStudent(STUDENT_PASSWORD);

        ResponseEntity<Map> response = patchProfile(studentToken,
                Map.of("displayName", "Impostor"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(profiles.findByUserId(teacherId).orElseThrow().displayName())
                .isEqualTo("Ana Serrano");
    }

    @Test
    void rejectsATimeZoneThatIsNotReal() {
        ResponseEntity<Map> response = patchProfile(tokenOf(TEACHER_EMAIL, STUDENT_PASSWORD),
                Map.of("timezone", "Madrid/Spain"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(profiles.findByUserId(teacherId).orElseThrow().timezone().getId())
                .isEqualTo("Europe/Madrid");
    }

    // --- helpers ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> patchProfile(String token, Map<String, String> body) {
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/v1/teacher/profile", HttpMethod.PATCH,
                new HttpEntity<>(body, headers), Map.class);
    }
}
