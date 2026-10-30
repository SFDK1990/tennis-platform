package com.tennisplatform.pass;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The teacher's pass types, the switch and the passes they record (41-fase27-analisis-bonos.md, decisions 2, 3 and 6). */
class PassTypesApiTest extends AbstractPassTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void theTeacherCreatesChangesAndRetiresATypeAndAStudentNoLongerSeesItRetired() {
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        Student student = aStudentWhoMayBook();

        ResponseEntity<Map> changed = patch(type, Map.of("priceCents", 11_000, "neverExpires", true));
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody()).containsEntry("priceCents", 11_000).containsEntry("validityMonths", null)
                .containsEntry("lessons", 10).containsEntry("lessonType", "GROUP");
        assertThat(patch(type, Map.of("validityMonths", 6)).getBody()).containsEntry("validityMonths", 6);

        assertThat((List<Map>) types(student.token()).get("passTypes")).extracting(t -> t.get("name"))
                .containsExactly("10 de grupo");
        assertThat(patch(type, Map.of("retired", true)).getBody()).containsEntry("retired", true);

        assertThat((List<Map>) types(student.token()).get("passTypes")).isEmpty();
        assertThat((List<Map>) types(teacherToken).get("passTypes")).singleElement()
                .satisfies(t -> assertThat(t).containsEntry("retired", true));
        ResponseEntity<Map> retired = sell(student, type, today());
        assertThat(retired.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(retired.getBody()).containsEntry("code", "PASS_TYPE_RETIRED");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aTypeNobodyBoughtIsDeletedAndOneSomebodyBoughtIsOnlyRetired() {
        UUID unsold = aPassType("Prueba", "INDIVIDUAL", 1, null);
        UUID sold = aPassType("5 particulares", "INDIVIDUAL", 5, null);
        aPassOf(aStudentWhoMayBook(), sold);

        assertThat(delete(unsold).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<Map> inUse = delete(sold);
        assertThat(inUse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(inUse.getBody()).containsEntry("code", "PASS_TYPE_IN_USE");
        assertThat(delete(UUID.randomUUID()).getBody()).containsEntry("code", "PASS_TYPE_NOT_FOUND");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void refusesARepeatedNameAndValuesOutOfRange() {
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        ResponseEntity<Map> repeated = create(Map.of("name", "10 de grupo", "lessonType", "GROUP", "lessons", 5,
                "priceCents", 100));
        assertThat(repeated.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repeated.getBody()).containsEntry("code", "PASS_TYPE_ALREADY_EXISTS");

        assertThat(create(Map.of("name", "Cero", "lessonType", "GROUP", "lessons", 0, "priceCents", 100))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(create(Map.of("name", "Raro", "lessonType", "PADEL", "lessons", 5, "priceCents", 100))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(create(Map.of("name", "Negativo", "lessonType", "GROUP", "lessons", 5, "priceCents", -1))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(create(Map.of("name", "Eterno", "lessonType", "GROUP", "lessons", 5, "priceCents", 1,
                "validityMonths", 40)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(create(Map.of("name", " ", "lessonType", "GROUP", "lessons", 5, "priceCents", 1))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(patch(type, Map.of("lessonType", "PADEL")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(patch(UUID.randomUUID(), Map.of("name", "Nada")).getBody()).containsEntry("code", "PASS_TYPE_NOT_FOUND");
    }

    /** Decision 3: the pass copies its type, and lasts from the day it was paid. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void aPassCopiesItsTypeAndLastsFromTheDayItWasPaid() {
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        Student student = aStudentWhoMayBook();
        ResponseEntity<Map> sold = sell(student, type, today().minusDays(10));
        assertThat(sold.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        patch(type, Map.of("lessons", 20, "priceCents", 1));

        Map pass = passOf(student, UUID.fromString((String) sold.getBody().get("id")));
        assertThat(pass).containsEntry("name", "10 de grupo").containsEntry("lessons", 10)
                .containsEntry("priceCents", 10_000).containsEntry("remaining", 10).containsEntry("used", 0)
                .containsEntry("paymentMethod", "BIZUM").containsEntry("status", "ACTIVE")
                .containsEntry("paidOn", today().minusDays(10).toString())
                .containsEntry("validUntil", today().minusDays(10).plusMonths(3).toString());

        List<Map> ofTheStudent = (List<Map>) rest.exchange("/api/v1/teacher/passes?studentUserId=" + student.id(),
                HttpMethod.GET, new HttpEntity<>(bearer(teacherToken)), Map.class).getBody().get("items");
        assertThat(ofTheStudent).extracting(p -> p.get("id")).containsExactly(sold.getBody().get("id"));
        aPassOf(aStudentWhoMayBook(), type);
        List<Map> all = (List<Map>) rest.exchange("/api/v1/teacher/passes", HttpMethod.GET,
                new HttpEntity<>(bearer(teacherToken)), Map.class).getBody().get("items");
        assertThat(all).hasSize(2);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void onlyAStudentOfTheTeacherGetsAPassAndOnlyOfAnExistingType() {
        UUID type = aPassType("10 de grupo", "GROUP", 10, null);
        Student stranger = aStudentWithAProfile();
        ResponseEntity<Map> notManaged = sell(stranger, type, today());
        assertThat(notManaged.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(notManaged.getBody()).containsEntry("code", "STUDENT_NOT_MANAGED");

        ResponseEntity<Map> noType = sell(aStudentWhoMayBook(), UUID.randomUUID(), today());
        assertThat(noType.getBody()).containsEntry("code", "PASS_TYPE_NOT_FOUND");

        ResponseEntity<Map> badMethod = rest.exchange("/api/v1/teacher/students/" + aStudentWhoMayBook().id()
                + "/passes", HttpMethod.POST, new HttpEntity<>(Map.of("passTypeId", type, "paidOn",
                today().toString(), "paymentMethod", "CHEQUE"), jsonBearer(teacherToken)), Map.class);
        assertThat(badMethod.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /** Decision 2: off until the teacher switches it on, and anybody can read whether it is. */
    @Test
    void theSwitchIsOffUntilTheTeacherTurnsItOn() {
        Student student = aStudentWhoMayBook();
        assertThat(types(student.token())).containsEntry("required", false);

        requirePasses(true);

        assertThat(types(student.token())).containsEntry("required", true);
        assertThat(rest.exchange("/api/v1/me/passes", HttpMethod.GET, new HttpEntity<>(bearer(student.token())),
                Map.class).getBody()).containsEntry("required", true).containsEntry("items", List.of());
        requirePasses(false);
        assertThat(types(student.token())).containsEntry("required", false);
    }

    /**
     * Fase 27.5, decision 6: without Stripe's keys in the environment, which is how the suite
     * runs, online payment cannot be switched on, and everybody can read why.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void onlinePaymentCannotBeSwitchedOnWithoutTheGatewaysKeys() {
        Student student = aStudentWhoMayBook();
        assertThat(types(student.token())).containsEntry("onlinePayment", false)
                .containsEntry("onlinePaymentAvailable", false);

        ResponseEntity<Map> refused = rest.exchange("/api/v1/teacher/pass-settings", HttpMethod.PUT,
                new HttpEntity<>(Map.of("onlinePayment", true), jsonBearer(teacherToken)), Map.class);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refused.getBody()).containsEntry("code", "ONLINE_PAYMENT_UNAVAILABLE");
        assertThat(types(student.token())).containsEntry("onlinePayment", false);
        ResponseEntity<Map> buy = rest.exchange("/api/v1/me/pass-purchases", HttpMethod.POST,
                new HttpEntity<>(Map.of("passTypeId", aPassType("5 particulares", "INDIVIDUAL", 5, null)),
                        jsonBearer(student.token())), Map.class);
        assertThat(buy.getBody()).containsEntry("code", "ONLINE_PAYMENT_DISABLED");
    }

    /** A pass recorded by hand is never "paid online": only the gateway's notice says that. */
    @Test
    @SuppressWarnings("rawtypes")
    void theTeacherCannotRecordAPassAsPaidOnline() {
        Student student = aStudentWhoMayBook();
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);

        ResponseEntity<Map> online = rest.exchange("/api/v1/teacher/students/" + student.id() + "/passes",
                HttpMethod.POST, new HttpEntity<>(Map.of("passTypeId", type, "paidOn", today().toString(),
                        "paymentMethod", "ONLINE"), jsonBearer(teacherToken)), Map.class);

        assertThat(online.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @SuppressWarnings("rawtypes")
    private Map types(String token) {
        return rest.exchange("/api/v1/pass-types", HttpMethod.GET, new HttpEntity<>(bearer(token)), Map.class).getBody();
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> create(Map<String, Object> body) {
        return rest.exchange("/api/v1/teacher/pass-types", HttpMethod.POST,
                new HttpEntity<>(new HashMap<>(body), jsonBearer(teacherToken)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> patch(UUID type, Map<String, Object> body) {
        return rest.exchange("/api/v1/teacher/pass-types/" + type, HttpMethod.PATCH,
                new HttpEntity<>(body, jsonBearer(teacherToken)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> delete(UUID type) {
        return rest.exchange("/api/v1/teacher/pass-types/" + type, HttpMethod.DELETE,
                new HttpEntity<>(bearer(teacherToken)), Map.class);
    }
}
