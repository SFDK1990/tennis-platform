package com.tennisplatform.security;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.contract.ContractValidation;
import com.tennisplatform.lesson.domain.LessonType;
import io.swagger.v3.oas.models.PathItem;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every operation of openapi.yaml, called by six identities, against resources that belong to
 * {@code STUDENT} (26-fase15-analisis-seguridad.md). {@code OTHER} is a second student in exactly
 * the same situation, so each of their rows is a horizontal-access check: somebody else's
 * booking must answer as if it did not exist.
 *
 * <p>The module tests prove each rule; this proves no operation escapes them. An operation
 * without a row fails the build, as a route missing from the spec does.
 *
 * <p>{@link #ALLOWED} means the identity may use the operation. A read is then called and must
 * succeed; a write is not called, because it would change what the rest of the matrix runs
 * against, and its module test already covers it.
 */
class AccessMatrixTest extends AbstractBookingTest {

    private static final int ALLOWED = 0;

    /** The columns of every row, in this order. */
    private enum Who { ANONYMOUS, UNVERIFIED, STUDENT, OTHER, TEACHER, ADMIN }

    private record Row(String method, String path, Object body, int[] expected) {
    }

    private final Map<String, Row> matrix = new LinkedHashMap<>();

    private Map<Who, String> tokens;
    private Map<String, String> ids;

    private void row(String method, String path, Object body, int... expected) {
        assertThat(expected).hasSize(Who.values().length);
        matrix.put(method + " " + path, new Row(method, path, body, expected));
    }

    private void defineTheMatrix() {
        LocalDate day = LocalDate.now(ZoneId.of("Europe/Madrid")).plusDays(10);
        Object none = null;
        Map<String, Object> anyEmail = Map.of("email", "nobody@example.com", "password", "a-valid-password");

        //                                                                        ANON UNVER STUD OTHER TEACH ADMIN
        // Public by definition: how an identity is obtained. Nothing to deny.
        row("POST", "/auth/register", anyEmail,                                  ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        row("POST", "/auth/login", anyEmail,                                     ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        row("POST", "/auth/verify-email", Map.of("token", "x"),                  ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        row("POST", "/auth/forgot-password", Map.of("email", "a@example.com"),   ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        row("POST", "/auth/reset-password", Map.of("token", "x", "newPassword", "a-valid-password"),
                                                                                 ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        // Authenticated by the refresh cookie; without the CSRF header every caller is refused.
        row("POST", "/auth/logout", none,                                        403, 403, 403, 403, 403, 403);
        row("POST", "/auth/refresh", none,                                       403, 403, 403, 403, 403, 403);
        row("POST", "/auth/verification-email", none,                            401, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);

        row("GET", "/me", none,                                                  401, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        row("PATCH", "/me", Map.of("phone", "600000000"),                        401, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        row("GET", "/me/export", none,                                           401, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        row("POST", "/me/deletion", Map.of("password", PASSWORD),                401, ALLOWED, ALLOWED, ALLOWED, 403, 403);
        row("POST", "/me/password", Map.of("currentPassword", PASSWORD, "newPassword", "another-valid-password"),
                                                                                 401, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        row("GET", "/teacher/profile", none,                                     401, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);

        row("GET", "/teacher/students", none,                                    401, 403, 403, 403, ALLOWED, 403);
        row("GET", "/teacher/students/lookup?email={studentEmail}", none,        401, 403, 403, 403, ALLOWED, 403);
        row("GET", "/teacher/students/{studentId}", none,                        401, 403, 403, 403, ALLOWED, 403);
        row("POST", "/teacher/students/{studentId}/manage", none,                401, 403, 403, 403, ALLOWED, 403);
        row("DELETE", "/teacher/students/{studentId}/manage", none,              401, 403, 403, 403, ALLOWED, 403);

        row("GET", "/teacher/availability?from=" + day + "&to=" + day, none,     401, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        row("PUT", "/teacher/availability/weekly", Map.of("rules", List.of()),   401, 403, 403, 403, ALLOWED, 403);
        row("POST", "/teacher/availability/exceptions", Map.of("date", day.toString(), "type", "BLOCK"),
                                                                                 401, 403, 403, 403, ALLOWED, 403);
        row("DELETE", "/teacher/availability/exceptions/{exceptionId}", none,    401, 403, 403, 403, ALLOWED, 403);

        row("GET", "/calendar?from=" + day + "&to=" + day, none,                 401, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        row("POST", "/teacher/lessons", Map.of("type", "INDIVIDUAL", "capacity", 1,
                "startsAt", day + "T10:00:00Z", "endsAt", day + "T11:00:00Z"),   401, 403, 403, 403, ALLOWED, 403);
        row("GET", "/lessons/{lessonId}", none,                                  401, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        row("POST", "/lessons/{lessonId}/bookings", none,                        401, 403, ALLOWED, ALLOWED, 403, 403);
        row("GET", "/bookings", none,                                            401, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED);
        // Somebody else's booking does not exist for them.
        row("POST", "/bookings/{bookingId}/cancel", none,                        401, 404, ALLOWED, 404, ALLOWED, ALLOWED);
        row("POST", "/teacher/lessons/{lessonId}/cancel", none,                  401, 403, 403, 403, ALLOWED, 403);
        row("POST", "/teacher/lessons/{lessonId}/attendance",
                Map.of("entries", List.of(Map.of("bookingId", "{bookingId}", "status", "ATTENDED"))),
                                                                                 401, 403, 403, 403, ALLOWED, 403);

        row("GET", "/admin/configuration", none,                                 401, 403, 403, 403, 403, ALLOWED);
        row("PATCH", "/admin/configuration", Map.of("studentLimit", 40),         401, 403, 403, 403, 403, ALLOWED);
        row("POST", "/admin/lessons/{lessonId}/cancel", none,                   401, 403, 403, 403, 403, ALLOWED);
        row("GET", "/admin/users", none,                                         401, 403, 403, 403, 403, ALLOWED);
        row("PATCH", "/admin/users/{studentId}/status", Map.of("status", "DISABLED"),
                                                                                 401, 403, 403, 403, 403, ALLOWED);
    }

    @Test
    void everyOperationOfTheContractHasARow() {
        defineTheMatrix();
        Set<String> inTheMatrix = new TreeSet<>();
        matrix.values().forEach(row -> inTheMatrix.add(row.method() + " " + templateOf(row.path())));

        Set<String> inTheSpec = new TreeSet<>();
        ContractValidation.specification().getPaths().forEach((path, item) ->
                item.readOperationsMap().keySet().forEach((PathItem.HttpMethod method) ->
                        inTheSpec.add(method.name() + " " + path.replaceAll("\\{[^}]+}", "{}"))));

        assertThat(inTheMatrix).as("operations of openapi.yaml and rows of the access matrix")
                .isEqualTo(inTheSpec);
    }

    @Test
    void everyIdentityGetsWhatTheMatrixSaysOnEveryOperation() {
        defineTheMatrix();
        arrange();

        List<String> wrong = new ArrayList<>();
        for (Row row : matrix.values()) {
            for (Who who : Who.values()) {
                int expected = row.expected()[who.ordinal()];
                boolean read = row.method().equals("GET");
                if (expected == ALLOWED && !read) {
                    continue;
                }
                int actual = call(row, who).getStatusCode().value();
                boolean ok = expected == ALLOWED ? actual / 100 == 2 : actual == expected;
                if (!ok) {
                    wrong.add("%-7s %-50s %-10s expected %s, got %d".formatted(row.method(), row.path(), who,
                            expected == ALLOWED ? "2xx" : expected, actual));
                }
            }
        }
        assertThat(wrong).as("cells of the access matrix that do not hold").isEmpty();
    }

    /** The list is where a horizontal leak would hide: a 200 that carries somebody else's rows. */
    @Test
    @SuppressWarnings("rawtypes")
    void aStudentsListOfBookingsHoldsNobodyElses() {
        defineTheMatrix();
        arrange();

        assertThat(bookingsSeenBy(Who.STUDENT)).hasSize(1);
        assertThat(bookingsSeenBy(Who.OTHER)).isEmpty();
    }

    @SuppressWarnings("rawtypes")
    private List<?> bookingsSeenBy(Who who) {
        ResponseEntity<Map> page = rest.exchange("/api/v1/bookings?lessonId=" + ids.get("lessonId"),
                HttpMethod.GET, new HttpEntity<>(bearer(tokens.get(who))), Map.class);
        return (List<?>) page.getBody().get("items");
    }

    @SuppressWarnings("rawtypes")
    private void arrange() {
        Student student = aStudentWhoMayBook();
        Student other = aStudentWhoMayBook();
        Student unverified = aStudentWithAProfile();
        Student admin = aStudentWithAProfile();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", admin.id());

        UUID lessonId = aLessonStartingIn(Duration.ofDays(3), LessonType.GROUP, 4);
        ResponseEntity<Map> booked = book(student.token(), lessonId);
        ResponseEntity<Map> exception = rest.exchange("/api/v1/teacher/availability/exceptions", HttpMethod.POST,
                new HttpEntity<>(Map.of("date", LocalDate.now(ZoneId.of("Europe/Madrid")).plusDays(20).toString(),
                        "type", "BLOCK"), jsonBearer(teacherToken)), Map.class);

        tokens = new LinkedHashMap<>();
        tokens.put(Who.ANONYMOUS, null);
        tokens.put(Who.UNVERIFIED, unverified.token());
        tokens.put(Who.STUDENT, student.token());
        tokens.put(Who.OTHER, other.token());
        tokens.put(Who.TEACHER, teacherToken);
        tokens.put(Who.ADMIN, tokenOf(admin.email(), PASSWORD));

        ids = Map.of(
                "studentId", student.id().toString(),
                "studentEmail", student.email(),
                "lessonId", lessonId.toString(),
                "bookingId", (String) booked.getBody().get("id"),
                "exceptionId", (String) exception.getBody().get("id"));
    }

    private ResponseEntity<String> call(Row row, Who who) {
        HttpHeaders headers = new HttpHeaders();
        String token = tokens.get(who);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        Object body = row.body() == null ? null : fill(row.body());
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return rest.exchange("/api/v1" + fill(row.path()), HttpMethod.valueOf(row.method()),
                new HttpEntity<>(body, headers), String.class);
    }

    private Object fill(Object value) {
        if (value instanceof String text) {
            String filled = text;
            for (var id : ids.entrySet()) {
                filled = filled.replace("{" + id.getKey() + "}", id.getValue());
            }
            return filled;
        }
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> filled = new LinkedHashMap<>();
            map.forEach((key, item) -> filled.put(key, fill(item)));
            return filled;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::fill).toList();
        }
        return value;
    }

    private static String templateOf(String path) {
        return path.replaceAll("\\?.*", "").replaceAll("\\{[^}]+}", "{}");
    }
}
