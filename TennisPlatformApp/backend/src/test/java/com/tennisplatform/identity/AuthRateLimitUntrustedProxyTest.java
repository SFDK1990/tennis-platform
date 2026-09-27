package com.tennisplatform.identity;

import com.tennisplatform.AbstractIntegrationTest;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Here 127.0.0.1 is not a trusted proxy, so the caller is a stranger and its X-Forwarded-For
 * is its own claim. Believing it would give an attacker a fresh allowance per request.
 */
@TestPropertySource(properties = {
    "tennis.identity.auth-rate-limit-per-minute=3",
    "tennis.identity.trusted-proxies=10.0.0.0/8"
})
class AuthRateLimitUntrustedProxyTest extends AbstractIntegrationTest {

    /** See AuthRateLimitTest: the default client would retry the 429 after Retry-After. */
    @BeforeEach
    void useANonRetryingClient() {
        rest.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory(
                HttpClients.custom().disableAutomaticRetries().build()));
    }

    @Test
    void aForwardedForFromOutsideTheTrustedProxiesIsIgnored() {
        for (int i = 0; i < 3; i++) {
            assertThat(attemptClaiming("203.0.113." + i).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }

        assertThat(attemptClaiming("203.0.113.99").getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    private ResponseEntity<String> attemptClaiming(String address) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", address);
        return rest.postForEntity("/api/v1/auth/forgot-password",
                new HttpEntity<>(Map.of("email", "someone@example.com"), headers), String.class);
    }
}
