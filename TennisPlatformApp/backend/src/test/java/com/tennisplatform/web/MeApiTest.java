package com.tennisplatform.web;

import com.tennisplatform.AbstractIntegrationTest;
import com.tennisplatform.identity.application.port.in.ProvisionTeacherAccount;
import com.tennisplatform.teacher.application.port.out.TeacherProfileRepository;
import com.tennisplatform.teacher.domain.TeacherProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code /me} over real HTTP: the endpoint composed at the web edge out of three modules.
 *
 * <p>What is worth testing here is precisely what the composition could get wrong - answering
 * with somebody else's data, applying a field that belongs to another role, or letting the
 * account itself be edited through a profile endpoint.
 */
class MeApiTest extends AbstractIntegrationTest {

    private static final String TEACHER_EMAIL = "teacher@tennis-platform.local";
    private static final String PASSWORD = "a-valid-password";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ProvisionTeacherAccount accounts;

    @Autowired
    private TeacherProfileRepository teacherProfiles;

    @Autowired
    private Clock clock;

    private UUID teacherId;

    @BeforeEach
    void seedTheTeacher() {
        teacherId = accounts.provision(TEACHER_EMAIL, PASSWORD);
        teacherProfiles.save(TeacherProfile.create(teacherId, "Ana Serrano", null,
                "Europe/Madrid", clock.instant()));
    }

    /**
     * A verified student who has not filled their data in yet gets nulls, not a 404. The
     * frontend reads that absence as "ask them for their data"
     * (16-fase6-analisis-perfiles.md).
     */
    @Test
    void aStudentWithoutAProfileGetsTheAccountAndNullPersonalFields() {
        String token = tokenOfANewStudent();

        ResponseEntity<Map> me = get(token);

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).containsEntry("role", "STUDENT");
        assertThat(me.getBody()).containsEntry("status", "PENDING_VERIFICATION");
        assertThat(me.getBody().get("fullName")).isNull();
        assertThat(me.getBody().get("nationalId")).isNull();
        assertThat(me.getBody().get("address")).isNull();
    }

    @Test
    void aStudentFillsTheirDataInAndReadsItBack() {
        String token = tokenOfANewStudent();

        ResponseEntity<Map> saved = patch(token, Map.of("fullName", "Lucia Prieto",
                "phone", "600123123", "nationalId", "12345678Z", "address", "Calle Mayor 1"));

        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(saved.getBody()).containsEntry("fullName", "Lucia Prieto");
        assertThat(get(token).getBody()).containsEntry("address", "Calle Mayor 1");
    }

    @Test
    void theFirstSaveNeedsTheName() {
        String token = tokenOfANewStudent();

        ResponseEntity<Map> response = patch(token, Map.of("phone", "600123123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "STUDENT_PROFILE_INVALID");
    }

    /**
     * Criterion 4 of Fase 6. The request body carries a role, an email and a status; the
     * response proves none of them was applied. The DTO has no field for them, which is what
     * makes this impossible rather than merely checked.
     */
    @Test
    void theAccountItselfCannotBeEditedThroughTheProfile() {
        String token = tokenOfANewStudent();
        patch(token, Map.of("fullName", "Lucia Prieto"));
        String emailBefore = (String) get(token).getBody().get("email");

        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "Lucia P.");
        body.put("role", "ADMIN");
        body.put("email", "attacker@example.com");
        body.put("status", "ACTIVE");
        ResponseEntity<Map> response = patch(token, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("role", "STUDENT");
        assertThat(response.getBody()).containsEntry("status", "PENDING_VERIFICATION");
        assertThat(response.getBody()).containsEntry("email", emailBefore);
    }

    @Test
    void aStudentCannotSendFieldsThatBelongToTheTeacher() {
        String token = tokenOfANewStudent();

        ResponseEntity<Map> response = patch(token,
                Map.of("fullName", "Lucia Prieto", "timezone", "America/Bogota"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "FIELD_NOT_APPLICABLE_TO_ROLE");
    }

    @Test
    void theTeacherSeesTheirOwnProfileFields() {
        String token = tokenOf(TEACHER_EMAIL);

        ResponseEntity<Map> me = get(token);

        assertThat(me.getBody()).containsEntry("role", "TEACHER");
        assertThat(me.getBody()).containsEntry("displayName", "Ana Serrano");
        assertThat(me.getBody()).containsEntry("timezone", "Europe/Madrid");
        assertThat(me.getBody().get("nationalId")).isNull();
    }

    @Test
    void theTeacherUpdatesTheirProfileThroughMe() {
        String token = tokenOf(TEACHER_EMAIL);

        ResponseEntity<Map> response = patch(token,
                Map.of("displayName", "Ana S.", "timezone", "America/Bogota"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("displayName", "Ana S.");
        assertThat(response.getBody()).containsEntry("timezone", "America/Bogota");
    }

    @Test
    void theTeacherCannotSendFieldsThatBelongToAStudent() {
        String token = tokenOf(TEACHER_EMAIL);

        ResponseEntity<Map> response = patch(token, Map.of("nationalId", "12345678Z"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "FIELD_NOT_APPLICABLE_TO_ROLE");
    }

    @Test
    void anAnonymousCallerGetsNothing() {
        assertThat(rest.getForEntity("/api/v1/me", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** Two students never see each other: the id comes from the token, not from the request. */
    @Test
    void everyCallerSeesOnlyTheirOwnData() {
        String first = tokenOfANewStudent();
        String second = tokenOfANewStudent();
        patch(first, Map.of("fullName", "Lucia Prieto"));
        patch(second, Map.of("fullName", "Mario Gil"));

        assertThat(get(first).getBody()).containsEntry("fullName", "Lucia Prieto");
        assertThat(get(second).getBody()).containsEntry("fullName", "Mario Gil");
    }

    // --- helpers ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> get(String token) {
        return rest.exchange("/api/v1/me", HttpMethod.GET, new HttpEntity<>(bearer(token)),
                Map.class);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> patch(String token, Map<String, ?> body) {
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/v1/me", HttpMethod.PATCH, new HttpEntity<>(body, headers),
                Map.class);
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String tokenOfANewStudent() {
        String email = "student-" + UUID.randomUUID() + "@example.com";
        rest.postForEntity("/api/v1/auth/register",
                Map.of("email", email, "password", PASSWORD), String.class);
        return tokenOf(email);
    }

    @SuppressWarnings("unchecked")
    private String tokenOf(String email) {
        ResponseEntity<Map> login = rest.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", PASSWORD), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) login.getBody().get("accessToken");
    }
}
