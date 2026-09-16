package com.tennisplatform.identity;

import com.tennisplatform.AbstractIntegrationTest;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
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

    @Autowired
    private TestRestTemplate rest;

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
        assertThat(attempt(0).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(attempt(1).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(attempt(2).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ResponseEntity<String> throttled = attempt(3);

        assertThat(throttled.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(throttled.getHeaders().getFirst("Retry-After")).isEqualTo("60");
        assertThat(throttled.getBody()).contains("AUTH_RATE_LIMITED");
    }

    private ResponseEntity<String> attempt(int index) {
        return rest.postForEntity("/api/v1/auth/forgot-password",
                Map.of("email", "someone-" + index + "@example.com"), String.class);
    }
}
