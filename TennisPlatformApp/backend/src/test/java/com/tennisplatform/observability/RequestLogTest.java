package com.tennisplatform.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennisplatform.booking.AbstractBookingTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The log as production writes it: JSON in the Elastic Common Schema, one object per line
 * (28-fase16-analisis-observabilidad.md). A booking is followed through it by the correlation id
 * the client sent, and nothing in it says who the student is beyond their id.
 *
 * <p>Logback is configured once per JVM: a context started after another one keeps the format
 * the first chose, and this test passed alone and failed after any other class. So the logging
 * system is marked as not configured before this class's context starts - the static block runs
 * before Spring builds it - and again when the class is done, for the next context to set its own.
 */
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "logging.structured.format.console=ecs")
class RequestLogTest extends AbstractBookingTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    static {
        forgetTheLoggingConfiguration();
    }

    @AfterAll
    static void forgetTheLoggingConfiguration() {
        LoggingSystem.get(RequestLogTest.class.getClassLoader()).cleanUp();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aBookingCanBeFollowedThroughTheLogByItsCorrelationId(CapturedOutput output) {
        Student student = aStudentWhoMayBook();
        UUID lessonId = anIndividualLessonIn(Duration.ofDays(3));
        String correlationId = "fase16-" + UUID.randomUUID();

        HttpHeaders headers = bearer(student.token());
        headers.set("X-Correlation-Id", correlationId);
        ResponseEntity<Map> booked = rest.exchange("/api/v1/lessons/" + lessonId + "/bookings",
                HttpMethod.POST, new HttpEntity<>(headers), Map.class);
        assertThat(booked.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        List<JsonNode> lines = linesOf(output, correlationId);
        assertThat(lines).extracting(line -> line.path("message").asText())
                .anyMatch(message -> message.startsWith("Booking ") && message.contains("created in lesson " + lessonId))
                .anyMatch(message -> message.startsWith("POST /api/v1/lessons/{id}/bookings 201 none "));
        assertThat(lines).allSatisfy(line -> {
            assertThat(line.path("userId").asText()).isEqualTo(student.id().toString());
            assertThat(line.path("role").asText()).isEqualTo("STUDENT");
        });
    }

    /** 02-arquitectura.md: no passwords, tokens or personal data in the log, whatever the level. */
    @Test
    void theAccountAndBookingFlowsLeaveNoPersonalDataInTheLog(CapturedOutput output) {
        Student student = aStudentWhoMayBook();
        rest.postForEntity("/api/v1/auth/login",
                Map.of("email", student.email(), "password", "not-" + PASSWORD), String.class);
        assertThat(book(student.token(), anIndividualLessonIn(Duration.ofDays(4))).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        assertThat(output.getAll())
                .contains("POST /api/v1/auth/login 401 AUTH_INVALID_CREDENTIALS")
                .doesNotContain(student.email())
                .doesNotContain(PASSWORD)
                .doesNotContain("Lucia Prieto")
                .doesNotContain(student.token());
    }

    private static List<JsonNode> linesOf(CapturedOutput output, String correlationId) {
        return output.getAll().lines()
                .filter(line -> line.startsWith("{") && line.contains(correlationId))
                .map(RequestLogTest::parse)
                .filter(line -> correlationId.equals(line.path("correlationId").asText()))
                .toList();
    }

    private static JsonNode parse(String line) {
        try {
            return JSON.readTree(line);
        } catch (Exception e) {
            throw new AssertionError("Not a JSON log line: " + line, e);
        }
    }
}
