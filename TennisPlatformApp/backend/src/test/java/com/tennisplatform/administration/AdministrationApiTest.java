package com.tennisplatform.administration;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.identity.application.service.ProvisionAdminAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Criteria of 23-fase12-analisis-administracion.md, over real HTTP. */
@SuppressWarnings({"rawtypes", "unchecked"})
class AdministrationApiTest extends AbstractBookingTest {

    private static final String ADMIN_EMAIL = "admin@tennis-platform.local";

    @Autowired
    private ProvisionAdminAccountService admins;

    private String adminToken;

    @BeforeEach
    void anAdministrator() {
        admins.provision(ADMIN_EMAIL, PASSWORD);
        adminToken = tokenOf(ADMIN_EMAIL, PASSWORD);
    }

    /** It runs on every startup, so running it again must change nothing. */
    @Test
    void provisioningTheAdministratorTwiceLeavesOneAccount() {
        boolean createdAgain = admins.provision(ADMIN_EMAIL, "a-different-password");

        assertThat(createdAgain).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE role = 'ADMIN'", Integer.class)).isEqualTo(1);
        assertThat(tokenOf(ADMIN_EMAIL, PASSWORD)).isNotBlank();
    }

    @Test
    void theTeacherAndStudentsAreRefusedEverywhereUnderAdmin() {
        Student student = aStudentWithAProfile();
        for (String token : List.of(teacherToken, student.token())) {
            assertForbidden(call(token, HttpMethod.GET, "/api/v1/admin/configuration", null));
            assertForbidden(call(token, HttpMethod.PATCH, "/api/v1/admin/configuration", Map.of("studentLimit", 1)));
            assertForbidden(call(token, HttpMethod.GET, "/api/v1/admin/users", null));
            assertForbidden(call(token, HttpMethod.PATCH, "/api/v1/admin/users/" + student.id() + "/status",
                    Map.of("status", "DISABLED")));
        }
    }

    /** Lowering the limit lets nobody go; it only stops the next one. */
    @Test
    void aLowerLimitStopsTheNextStudentBeingManaged() {
        aStudentWhoMayBook();
        Student next = aStudentWithAProfile();

        ResponseEntity<Map> changed = call(adminToken, HttpMethod.PATCH, "/api/v1/admin/configuration",
                Map.of("studentLimit", 1));
        ResponseEntity<Map> refused = call(teacherToken, HttpMethod.POST,
                "/api/v1/teacher/students/" + next.id() + "/manage", null);

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody()).containsEntry("studentLimit", 1).containsEntry("maxGroupCapacity", 8);
        assertThat(changed.getBody().get("updatedBy")).isEqualTo(idOf(adminToken));
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(refused.getBody()).containsEntry("code", "STUDENT_LIMIT_REACHED");
    }

    @Test
    void theAdministratorReadsTheConfigurationAsSeeded() {
        ResponseEntity<Map> configuration = call(adminToken, HttpMethod.GET, "/api/v1/admin/configuration", null);

        assertThat(configuration.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(configuration.getBody()).containsEntry("studentLimit", 50).containsEntry("maxGroupCapacity", 8);
        assertThat(configuration.getBody().get("updatedBy")).isNull();
    }

    /** Only ACTIVE and DISABLED can be asked for; PENDING_VERIFICATION is earned by the address. */
    @Test
    void aStatusTheConsoleCannotSetIsABadRequest() {
        Student student = aStudentWithAProfile();

        ResponseEntity<Map> refused = changeStatus(student.id(), "PENDING_VERIFICATION");

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aLimitThatIsNotPositiveIsRefused() {
        ResponseEntity<Map> refused = call(adminToken, HttpMethod.PATCH, "/api/v1/admin/configuration",
                Map.of("maxGroupCapacity", 0));

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(refused.getBody()).containsEntry("code", "VALIDATION_ERROR");
    }

    /** Disabled, the student can neither get in nor keep a seat somebody else could take. */
    @Test
    void disablingAStudentLocksThemOutAndFreesTheirSeats() {
        Student student = aStudentWhoMayBook();
        UUID lesson = anIndividualLessonIn(Duration.ofDays(3));
        book(student.token(), lesson);

        ResponseEntity<Map> disabled = changeStatus(student.id(), "DISABLED");

        assertThat(disabled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(disabled.getBody()).containsEntry("status", "DISABLED");
        assertThat(login(student.email()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
                Integer.class, student.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM bookings WHERE lesson_id = ?", String.class, lesson))
                .isEqualTo("CANCELLED_BY_ADMIN");
        assertThat(confirmedBookingsOf(lesson)).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM teacher_students WHERE student_user_id = ?",
                String.class, student.id())).isEqualTo("INACTIVE");
    }

    /** Reactivating must not verify an address nobody confirmed, nor take the student back on. */
    @Test
    void reactivatingGivesBackTheFormerStatusButNotTheManagement() {
        Student verified = aStudentWhoMayBook();
        Student unverified = aStudentWithAProfile();
        changeStatus(verified.id(), "DISABLED");
        changeStatus(unverified.id(), "DISABLED");

        assertThat(changeStatus(verified.id(), "ACTIVE").getBody()).containsEntry("status", "ACTIVE");
        assertThat(changeStatus(unverified.id(), "ACTIVE").getBody()).containsEntry("status", "PENDING_VERIFICATION");
        assertThat(login(verified.email()).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("SELECT status FROM teacher_students WHERE student_user_id = ?",
                String.class, verified.id())).isEqualTo("INACTIVE");
    }

    /** Disabling the only teacher would switch the platform off; an admin could lock everybody out. */
    @Test
    void onlyStudentAccountsCanBeDisabled() {
        ResponseEntity<Map> teacher = changeStatus(teacherId, "DISABLED");
        ResponseEntity<Map> self = changeStatus(UUID.fromString(idOf(adminToken)), "DISABLED");
        ResponseEntity<Map> nobody = changeStatus(UUID.randomUUID(), "DISABLED");

        assertThat(teacher.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(teacher.getBody()).containsEntry("code", "ADMIN_TARGET_NOT_ALLOWED");
        assertThat(self.getBody()).containsEntry("code", "ADMIN_TARGET_NOT_ALLOWED");
        assertThat(nobody.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(nobody.getBody()).containsEntry("code", "USER_NOT_FOUND");
    }

    @Test
    void theListFiltersByRoleStatusAndPartOfTheAddress() {
        Student student = aStudentWithAProfile();
        String fragment = student.email().substring(8, 20).toUpperCase();

        List<Map<String, Object>> students = items(call(adminToken, HttpMethod.GET,
                "/api/v1/admin/users?role=STUDENT&status=PENDING_VERIFICATION&query=" + fragment, null));
        List<Map<String, Object>> teachers = items(call(adminToken, HttpMethod.GET, "/api/v1/admin/users?role=TEACHER", null));

        assertThat(students).extracting(user -> user.get("email")).containsExactly(student.email());
        assertThat(teachers).extracting(user -> user.get("role")).containsOnly("TEACHER");
        assertThat(call(adminToken, HttpMethod.GET, "/api/v1/admin/users?role=OWNER", null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Map> changeStatus(UUID id, String status) {
        return call(adminToken, HttpMethod.PATCH, "/api/v1/admin/users/" + id + "/status", Map.of("status", status));
    }

    private ResponseEntity<Map> call(String token, HttpMethod method, String path, Object body) {
        return rest.exchange(path, method, new HttpEntity<>(body, body == null ? bearer(token) : jsonBearer(token)),
                Map.class);
    }

    private ResponseEntity<Map> login(String email) {
        return rest.postForEntity("/api/v1/auth/login", Map.of("email", email, "password", PASSWORD), Map.class);
    }

    private String idOf(String token) {
        return (String) call(token, HttpMethod.GET, "/api/v1/me", null).getBody().get("id");
    }

    private static void assertForbidden(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("code", "AUTH_FORBIDDEN");
    }

    private static List<Map<String, Object>> items(ResponseEntity<Map> page) {
        return (List<Map<String, Object>>) page.getBody().get("items");
    }
}
