package com.tennisplatform.student;

import com.tennisplatform.booking.AbstractBookingTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code POST /me/deletion} (01-analisis-funcional.md §18, 30-fase19-analisis-cierre-mvp.md): the
 * account is emptied, not removed, so past bookings keep a name to show and nothing personal is
 * left.
 */
class DeleteMyAccountTest extends AbstractBookingTest {

    private static final String FULL_NAME = "Marta Quintanilla Robles";
    private static final String PHONE = "600111222";
    private static final String NATIONAL_ID = "12345678Z";
    private static final String ADDRESS = "Calle del Saque 15, Valladolid";

    private Student student;
    private UUID upcoming;
    private UUID past;

    @BeforeEach
    @SuppressWarnings("rawtypes")
    void aStudentWithEverythingFilledInAndTwoBookings() {
        student = aStudentWhoMayBook();
        ResponseEntity<Map> profile = rest.exchange("/api/v1/me", HttpMethod.PATCH, new HttpEntity<>(
                Map.of("fullName", FULL_NAME, "phone", PHONE, "nationalId", NATIONAL_ID, "address", ADDRESS),
                jsonBearer(student.token())), Map.class);
        assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);

        upcoming = anIndividualLessonIn(Duration.ofDays(3));
        past = anIndividualLessonIn(Duration.ofDays(5));
        assertThat(book(student.token(), upcoming).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(book(student.token(), past).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // It could not be booked once started, so it is moved to last week afterwards.
        jdbc.update("UPDATE lessons SET starts_at = starts_at - interval '12 days', ends_at = ends_at - interval '12 days' "
                + "WHERE id = ?", past);
        jdbc.update("UPDATE bookings SET lesson_starts_at = lesson_starts_at - interval '12 days', "
                + "lesson_ends_at = lesson_ends_at - interval '12 days' WHERE lesson_id = ?", past);
    }

    @Test
    void theStudentCanNoLongerSignInTheirUpcomingSeatIsFreedAndThePastOneStays() {
        assertThat(delete(student.token(), PASSWORD).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(login(student.email(), PASSWORD).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(statusOfBookingIn(upcoming)).isEqualTo("CANCELLED_BY_STUDENT");
        assertThat(confirmedBookingsOf(upcoming)).isZero();
        assertThat(statusOfBookingIn(past)).isEqualTo("CONFIRMED");
        assertThat(jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, student.id()))
                .isEqualTo("DELETED");
        assertThat(jdbc.queryForObject("SELECT status FROM teacher_students WHERE student_user_id = ?",
                String.class, student.id())).isEqualTo("INACTIVE");
    }

    /**
     * The profile stays, under a name that says what happened, so last week's lesson still has
     * somebody in it; and the student leaves the teacher's list like anybody no longer managed.
     */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theProfileIsLeftAsADeletedStudentAndLeavesTheTeachersList() {
        delete(student.token(), PASSWORD);

        assertThat(jdbc.queryForObject("SELECT full_name FROM student_profiles WHERE user_id = ?", String.class,
                student.id())).isEqualTo("Alumno eliminado");
        ResponseEntity<Map> students = rest.exchange("/api/v1/teacher/students", HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
        assertThat((List<Map<String, Object>>) students.getBody().get("items")).isEmpty();
    }

    /**
     * The criterion of the phase: every text column of every table is searched for each personal
     * value the student gave. A column added later that copies one would fail here.
     */
    @Test
    void nothingPersonalOfTheStudentIsLeftAnywhereInTheDatabase() {
        assertThat(placesHolding(student.email())).isNotEmpty();

        delete(student.token(), PASSWORD);

        for (String personal : List.of(student.email(), FULL_NAME, PHONE, NATIONAL_ID, ADDRESS)) {
            assertThat(placesHolding(personal)).as("columns still holding %s", personal).isEmpty();
        }
    }

    @Test
    void theAddressIsFreeToRegisterAgain() {
        delete(student.token(), PASSWORD);

        ResponseEntity<String> again = rest.postForEntity("/api/v1/auth/register",
                Map.of("email", student.email(), "password", PASSWORD), String.class);

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(login(student.email(), PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aWrongPasswordChangesNothing() {
        ResponseEntity<Map> refused = rest.exchange("/api/v1/me/deletion", HttpMethod.POST,
                new HttpEntity<>(Map.of("password", "not-" + PASSWORD), jsonBearer(student.token())), Map.class);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(refused.getBody()).containsEntry("code", "CURRENT_PASSWORD_INCORRECT");
        assertThat(login(student.email(), PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusOfBookingIn(upcoming)).isEqualTo("CONFIRMED");
        assertThat(placesHolding(PHONE)).isNotEmpty();
    }

    @Test
    void theTeacherCannotDeleteTheirAccount() {
        assertThat(delete(teacherToken, PASSWORD).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(login(TEACHER_EMAIL, PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /** DELETED is final: there is nobody to give the account back to. */
    @Test
    @SuppressWarnings("rawtypes")
    void anAdministratorCannotReactivateADeletedAccount() {
        Student admin = aStudentWithAProfile();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", admin.id());
        String adminToken = tokenOf(admin.email(), PASSWORD);
        delete(student.token(), PASSWORD);

        ResponseEntity<Map> reactivated = rest.exchange("/api/v1/admin/users/" + student.id() + "/status",
                HttpMethod.PATCH, new HttpEntity<>(Map.of("status", "ACTIVE"), jsonBearer(adminToken)), Map.class);

        assertThat(reactivated.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(reactivated.getBody()).containsEntry("code", "ACCOUNT_DELETED");
    }

    private ResponseEntity<String> delete(String token, String password) {
        return rest.exchange("/api/v1/me/deletion", HttpMethod.POST,
                new HttpEntity<>(Map.of("password", password), jsonBearer(token)), String.class);
    }

    private ResponseEntity<String> login(String email, String password) {
        return rest.postForEntity("/api/v1/auth/login", Map.of("email", email, "password", password), String.class);
    }

    private String statusOfBookingIn(UUID lessonId) {
        return jdbc.queryForObject("SELECT status FROM bookings WHERE lesson_id = ? AND student_user_id = ?",
                String.class, lessonId, student.id());
    }

    /** "table.column" for every text column, in any table, that contains the value. */
    private List<String> placesHolding(String value) {
        List<String> places = new ArrayList<>();
        jdbc.query("SELECT table_name, column_name FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND data_type IN ('character varying', 'text') "
                + "AND table_name NOT LIKE 'databasechangelog%'", row -> {
                    String table = row.getString("table_name");
                    String column = row.getString("column_name");
                    Integer found = jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE "
                            + column + " ILIKE '%' || ? || '%'", Integer.class, value);
                    if (found != null && found > 0) {
                        places.add(table + "." + column);
                    }
                });
        return places;
    }
}
