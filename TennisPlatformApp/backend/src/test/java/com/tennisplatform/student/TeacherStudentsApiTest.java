package com.tennisplatform.student;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Managing students over real HTTP, which is where the role and the relationship are actually
 * enforced. Five of the seven acceptance criteria of Fase 6 are demonstrated here.
 */
class TeacherStudentsApiTest extends AbstractIntegrationTest {

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

    private String teacherToken;

    /** The base class emptied the database, so the teacher the bootstrap made is gone. */
    @BeforeEach
    void seedTheTeacher() {
        UUID teacherId = accounts.provision(TEACHER_EMAIL, PASSWORD);
        teacherProfiles.save(TeacherProfile.create(teacherId, "Ana Serrano", null,
                "Europe/Madrid", clock.instant()));
        teacherToken = tokenOf(TEACHER_EMAIL);
    }

    @Test
    void theTeacherManagesAStudentAndSeesThemListed() {
        Student student = aStudentWithAProfile("Lucia Prieto");

        ResponseEntity<Map> managed = manage(teacherToken, student.id());
        assertThat(managed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(managed.getBody()).containsEntry("managedStatus", "MANAGED");

        assertThat(namesInTheList(teacherToken)).containsExactly("Lucia Prieto");
    }

    /** Criterion 3 of Fase 6: managing twice answers 409 and does not create a second row. */
    @Test
    void managingTheSameStudentTwiceIsAConflict() {
        Student student = aStudentWithAProfile("Lucia Prieto");
        manage(teacherToken, student.id());

        ResponseEntity<Map> second = manage(teacherToken, student.id());

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).containsEntry("code", "STUDENT_ALREADY_MANAGED");
        assertThat(namesInTheList(teacherToken)).containsExactly("Lucia Prieto");
    }

    /**
     * Criterion 7 of Fase 6: a deactivated student drops out of the list, and taking them back
     * reuses the relationship rather than creating a second one - which the list would show as
     * a duplicate row if it happened.
     */
    @Test
    void aDeactivatedStudentLeavesTheListAndCanBeTakenBackOnce() {
        Student student = aStudentWithAProfile("Lucia Prieto");
        manage(teacherToken, student.id());

        ResponseEntity<Void> deactivated = stopManaging(teacherToken, student.id());
        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(namesInTheList(teacherToken)).isEmpty();

        assertThat(manage(teacherToken, student.id()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(namesInTheList(teacherToken)).containsExactly("Lucia Prieto");
    }

    @Test
    void deactivatingTwiceIsAConflict() {
        Student student = aStudentWithAProfile("Lucia Prieto");
        manage(teacherToken, student.id());
        stopManaging(teacherToken, student.id());

        assertThat(stopManaging(teacherToken, student.id()).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    /** Criterion 6 of Fase 6: the look-up that precedes managing takes no partial matches. */
    @Test
    void theLookupRefusesAnythingButTheCompleteAddress() {
        Student student = aStudentWithAProfile("Lucia Prieto");
        String email = student.email();

        assertThat(lookup(teacherToken, email).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lookup(teacherToken, email.substring(0, 8)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(lookup(teacherToken, "lucia").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void theLookupIgnoresTheCaseOfTheAddress() {
        Student student = aStudentWithAProfile("Lucia Prieto");

        ResponseEntity<Map> found = lookup(teacherToken, student.email().toUpperCase());

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody()).containsEntry("userId", student.id().toString());
    }

    /**
     * Criterion 2 of Fase 6: restricted personal data never reaches a teacher who does not
     * manage that student, and knowing the UUID does not help.
     */
    @Test
    void theTeacherCannotReadTheRestrictedDataOfAStudentTheyDoNotManage() {
        Student student = aStudentWithAProfile("Lucia Prieto");

        ResponseEntity<Map> detail = detail(teacherToken, student.id());

        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(detail.getBody()).containsEntry("code", "STUDENT_NOT_MANAGED");
    }

    @Test
    void theTeacherReadsTheRestrictedDataOfAStudentTheyDoManage() {
        Student student = aStudentWithAProfile("Lucia Prieto");
        manage(teacherToken, student.id());

        ResponseEntity<Map> detail = detail(teacherToken, student.id());

        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody()).containsEntry("nationalId", "12345678Z");
        assertThat(detail.getBody()).containsEntry("address", "Calle Mayor 1");
    }

    /** Restricted data is out of listings by construction, managed student or not. */
    @Test
    void theListingCarriesNoRestrictedData() {
        Student student = aStudentWithAProfile("Lucia Prieto");
        manage(teacherToken, student.id());

        ResponseEntity<Map> list = rest.exchange("/api/v1/teacher/students", HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class);

        assertThat(list.getBody().toString())
                .doesNotContain("12345678Z")
                .doesNotContain("Calle Mayor 1");
    }

    /**
     * Criterion 1 of Fase 6: a student asking about another student is refused, even holding
     * their UUID. These endpoints are the teacher's.
     */
    @Test
    void aStudentCannotAskAboutAnotherStudent() {
        Student target = aStudentWithAProfile("Lucia Prieto");
        Student nosy = aStudentWithAProfile("Mario Gil");

        assertThat(detail(nosy.token(), target.id()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(manage(nosy.token(), target.id()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(lookup(nosy.token(), target.email()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void anAnonymousCallerGetsNothing() {
        assertThat(rest.getForEntity("/api/v1/teacher/students", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aStudentWhoHasNotFilledTheirDataInCannotBeManagedYet() {
        Student student = aRegisteredStudent();

        ResponseEntity<Map> response = manage(teacherToken, student.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).containsEntry("code", "STUDENT_PROFILE_INCOMPLETE");
    }

    @Test
    void theSearchAmongMyStudentsIsPartialAndStaysAmongMine() {
        Student mine = aStudentWithAProfile("Lucia Prieto");
        Student notMine = aStudentWithAProfile("Luciano Vega");
        manage(teacherToken, mine.id());

        assertThat(namesInTheList(teacherToken, "luci")).containsExactly("Lucia Prieto");
        assertThat(namesInTheList(teacherToken, "Luciano")).isEmpty();
        assertThat(notMine.id()).isNotNull();
    }

    // --- helpers ---------------------------------------------------------------

    private record Student(UUID id, String email, String token) {
    }

    /** Registers, signs in and fills the personal data in, which is what creates the profile. */
    private Student aStudentWithAProfile(String fullName) {
        Student student = aRegisteredStudent();

        HttpHeaders headers = bearer(student.token());
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> saved = rest.exchange("/api/v1/me", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("fullName", fullName, "nationalId", "12345678Z",
                        "address", "Calle Mayor 1"), headers), Map.class);
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);

        return student;
    }

    @SuppressWarnings("unchecked")
    private Student aRegisteredStudent() {
        String email = "student-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<Map> registered = rest.postForEntity("/api/v1/auth/register",
                Map.of("email", email, "password", PASSWORD), Map.class);
        assertThat(registered.getStatusCode().is2xxSuccessful()).isTrue();

        String token = tokenOf(email);
        ResponseEntity<Map> me = rest.exchange("/api/v1/me", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), Map.class);
        return new Student(UUID.fromString((String) me.getBody().get("id")), email, token);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> manage(String token, UUID studentId) {
        return rest.exchange("/api/v1/teacher/students/" + studentId + "/manage",
                HttpMethod.POST, new HttpEntity<>(bearer(token)), Map.class);
    }

    private ResponseEntity<Void> stopManaging(String token, UUID studentId) {
        return rest.exchange("/api/v1/teacher/students/" + studentId + "/manage",
                HttpMethod.DELETE, new HttpEntity<>(bearer(token)), Void.class);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> lookup(String token, String email) {
        return rest.exchange("/api/v1/teacher/students/lookup?email={email}", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), Map.class, email);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> detail(String token, UUID studentId) {
        return rest.exchange("/api/v1/teacher/students/" + studentId, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), Map.class);
    }

    private List<String> namesInTheList(String token) {
        return namesInTheList(token, null);
    }

    @SuppressWarnings("unchecked")
    private List<String> namesInTheList(String token, String query) {
        String url = query == null
                ? "/api/v1/teacher/students"
                : "/api/v1/teacher/students?query=" + query;
        ResponseEntity<Map> response = rest.exchange(url, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody()
                .get("items");
        return items.stream().map(item -> (String) item.get("fullName")).toList();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private String tokenOf(String email) {
        ResponseEntity<Map> login = rest.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", PASSWORD), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) login.getBody().get("accessToken");
    }
}
