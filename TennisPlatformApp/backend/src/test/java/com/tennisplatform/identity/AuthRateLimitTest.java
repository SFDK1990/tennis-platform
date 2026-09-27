package com.tennisplatform.identity;

import com.tennisplatform.AbstractIntegrationTest;
import com.tennisplatform.observability.RequestMetricsTest;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lowers the limit to something a test can reach. The rest of the suite runs with it
 * effectively disabled, because every test calls from 127.0.0.1 and would otherwise throttle
 * itself rather than an attacker.
 *
 * <p>Uses forgot-password rather than login on purpose: login hashes a password on every
 * attempt, which is slow by design, and the allowance would partly refill while the test was
 * still running.
 */
@TestPropertySource(properties = "tennis.identity.auth-rate-limit-per-minute=3")
class AuthRateLimitTest extends AbstractIntegrationTest {

    private static final String[] THROTTLED = {"status", "429", "code", "AUTH_RATE_LIMITED"};

    @Autowired
    private MeterRegistry registry;

    /**
     * Apache HttpClient honours Retry-After and silently retries a 429 sixty seconds later,
     * by which point the allowance has refilled and the test sees the retry instead of the
     * rejection. Turning retries off is what lets the test observe what the server actually
     * answered.
     */
    @BeforeEach
    void useANonRetryingClient() {
        rest.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom().disableAutomaticRetries().build()));
    }

    @Test
    void repeatedAttemptsFromTheSameClientAreThrottled() {
        long throttledBefore = RequestMetricsTest.count(registry, THROTTLED);
        assertThat(attempt(0).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(attempt(1).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(attempt(2).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ResponseEntity<String> throttled = attempt(3);

        assertThat(throttled.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(throttled.getHeaders().getFirst("Retry-After")).isEqualTo("60");
        assertThat(throttled.getBody()).contains("AUTH_RATE_LIMITED");
        // Answered before the security chain and MVC, and still counted with its code.
        RequestMetricsTest.awaitCount(registry, throttledBefore + 1, THROTTLED);
    }

    /**
     * The test calls from 127.0.0.1, a trusted proxy by default, which is exactly where the
     * frontend sits. Two users behind it must not share one allowance
     * (26-fase15-analisis-seguridad.md).
     */
    @Test
    void twoClientsBehindTheFrontendEachHaveTheirOwnAllowance() {
        for (int i = 0; i < 3; i++) {
            assertThat(attemptFrom("203.0.113.10", i).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(attemptFrom("203.0.113.20", i).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }

        assertThat(attemptFrom("203.0.113.10", 3).getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    /** The client can write anything to the left; only the hop our own proxy appended counts. */
    @Test
    void whatTheClientWroteBeforeTheProxyAppendedItsHopDoesNotBuyMoreAttempts() {
        for (int i = 0; i < 3; i++) {
            attemptFrom("198.51.100." + i + ", 203.0.113.30", i);
        }

        assertThat(attemptFrom("198.51.100.99, 203.0.113.30", 3).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    private ResponseEntity<String> attempt(int index) {
        return rest.postForEntity("/api/v1/auth/forgot-password",
                Map.of("email", "someone-" + index + "@example.com"), String.class);
    }

    private ResponseEntity<String> attemptFrom(String forwardedFor, int index) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", forwardedFor);
        return rest.postForEntity("/api/v1/auth/forgot-password",
                new HttpEntity<>(Map.of("email", "someone-" + index + "@example.com"), headers), String.class);
    }
}
