package com.tennisplatform.observability;

import com.tennisplatform.booking.AbstractBookingTest;
import com.tennisplatform.identity.application.service.ProvisionAdminAccountService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The metrics of 28-fase16-analisis-observabilidad.md: {@code http.server.requests} tagged with
 * the error code, so a lost last seat is told apart from any other 409, and readable only by
 * the admin.
 */
public class RequestMetricsTest extends AbstractBookingTest {

    private static final String REQUESTS = "http.server.requests";
    private static final String BOOKINGS = "/api/v1/lessons/{id}/bookings";

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private ProvisionAdminAccountService admins;

    @Test
    void aLostLastSeatIsCountedWithItsCode() {
        UUID lessonId = anIndividualLessonIn(Duration.ofDays(5));
        String[] lost = {"uri", BOOKINGS, "status", "409", "code", "LESSON_FULL"};
        String[] booked = {"uri", BOOKINGS, "status", "201", "code", "none"};
        long lostBefore = count(registry, lost);
        long bookedBefore = count(registry, booked);

        assertThat(book(aStudentWhoMayBook().token(), lessonId).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(book(aStudentWhoMayBook().token(), lessonId).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        awaitCount(registry, lostBefore + 1, lost);
        awaitCount(registry, bookedBefore + 1, booked);
    }

    @Test
    void onlyTheAdminReadsTheMetrics() {
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.com";
        admins.provision(adminEmail, PASSWORD);

        assertThat(metricsAs(null)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(metricsAs(aStudentWhoMayBook().token())).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(metricsAs(teacherToken)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(metricsAs(tokenOf(adminEmail, PASSWORD))).isEqualTo(HttpStatus.OK);
    }

    private HttpStatus metricsAs(String token) {
        HttpEntity<?> request = token == null ? HttpEntity.EMPTY : new HttpEntity<>(bearer(token));
        return HttpStatus.valueOf(rest.exchange("/actuator/metrics/" + REQUESTS, HttpMethod.GET, request,
                String.class).getStatusCode().value());
    }

    /** Requests answered so far with these tags, whatever the other tags say. */
    public static long count(MeterRegistry registry, String... tags) {
        return registry.find(REQUESTS).tags(tags).timers().stream().mapToLong(Timer::count).sum();
    }

    /**
     * The response reaches the client before the filter that times it stops the clock, so the
     * count can lag the answer by a moment.
     */
    public static void awaitCount(MeterRegistry registry, long expected, String... tags) {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (count(registry, tags) < expected && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertThat(count(registry, tags)).as(String.join(" ", tags)).isEqualTo(expected);
    }
}
