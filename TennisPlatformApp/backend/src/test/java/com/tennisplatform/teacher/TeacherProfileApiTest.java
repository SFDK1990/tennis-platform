package com.tennisplatform.teacher;

import com.tennisplatform.AbstractIntegrationTest;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reading the teacher profile over real HTTP. Writing it goes through {@code PATCH /me}, tested
 * in {@code MeApiTest}.
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

    /**
     * Seeds the teacher through the public ports rather than with SQL, so the test exercises the
     * same path the bootstrap uses. The base class emptied the database first.
     */
    @BeforeEach
    void seedTheTeacher() {
        UUID teacherId = accounts.provision(TEACHER_EMAIL, STUDENT_PASSWORD);
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
}
