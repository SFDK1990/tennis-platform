package com.tennisplatform.pass;

import com.tennisplatform.booking.AbstractBookingTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** What every pass test needs: pass types, passes sold, the switch, and what is left of a pass. */
abstract class AbstractPassTest extends AbstractBookingTest {

    protected static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    protected LocalDate today() {
        return LocalDate.now(clock.withZone(MADRID));
    }

    /** A pass type; {@code validityMonths} null never expires. */
    @SuppressWarnings("rawtypes")
    protected UUID aPassType(String name, String lessonType, int lessons, Integer validityMonths) {
        Map<String, Object> body = new HashMap<>(Map.of("name", name, "lessonType", lessonType, "lessons", lessons,
                "priceCents", 1_000 * lessons));
        body.put("validityMonths", validityMonths);
        ResponseEntity<Map> created = rest.exchange("/api/v1/teacher/pass-types", HttpMethod.POST,
                new HttpEntity<>(body, jsonBearer(teacherToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) created.getBody().get("id"));
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> sell(Student student, UUID passTypeId, LocalDate paidOn) {
        return rest.exchange("/api/v1/teacher/students/" + student.id() + "/passes", HttpMethod.POST,
                new HttpEntity<>(Map.of("passTypeId", passTypeId, "paidOn", paidOn.toString(), "paymentMethod", "BIZUM"),
                        jsonBearer(teacherToken)), Map.class);
    }

    /** A pass paid today. */
    @SuppressWarnings("rawtypes")
    protected UUID aPassOf(Student student, UUID passTypeId) {
        return aPassOf(student, passTypeId, today());
    }

    @SuppressWarnings("rawtypes")
    protected UUID aPassOf(Student student, UUID passTypeId, LocalDate paidOn) {
        ResponseEntity<Map> sold = sell(student, passTypeId, paidOn);
        assertThat(sold.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) sold.getBody().get("id"));
    }

    @SuppressWarnings("rawtypes")
    protected void requirePasses(boolean required) {
        ResponseEntity<Map> set = rest.exchange("/api/v1/teacher/pass-settings", HttpMethod.PUT,
                new HttpEntity<>(Map.of("required", required), jsonBearer(teacherToken)), Map.class);
        assertThat(set.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /** The pass as the student reads it. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Map passOf(Student student, UUID passId) {
        List<Map> mine = (List<Map>) rest.exchange("/api/v1/me/passes", HttpMethod.GET,
                new HttpEntity<>(bearer(student.token())), Map.class).getBody().get("items");
        return mine.stream().filter(pass -> passId.toString().equals(pass.get("id"))).findFirst().orElseThrow();
    }

    protected int remainingOn(Student student, UUID passId) {
        return (Integer) passOf(student, passId).get("remaining");
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> cancel(String token, Object bookingId) {
        return rest.exchange("/api/v1/bookings/" + bookingId + "/cancel", HttpMethod.POST,
                new HttpEntity<>(bearer(token)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    protected ResponseEntity<Map> join(String token, UUID lessonId) {
        return rest.exchange("/api/v1/lessons/" + lessonId + "/waiting-list", HttpMethod.POST,
                new HttpEntity<>(bearer(token)), Map.class);
    }

    protected String anAdminToken() {
        Student admin = aStudentWithAProfile();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE id = ?", admin.id());
        return tokenOf(admin.email(), PASSWORD);
    }
}
