package com.tennisplatform.identity;

import com.tennisplatform.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two reset links pending at once. The mail cooldown is off here, because with it a second link
 * could not be asked for within the test.
 */
@Import(PasswordResetLinksTest.MailerStub.class)
@TestPropertySource(properties = "tennis.identity.mail-cooldown=0s")
class PasswordResetLinksTest extends AbstractIntegrationTest {

    @TestConfiguration
    static class MailerStub {

        @Bean
        @Primary
        RecordingMailer recordingMailer() {
            return new RecordingMailer();
        }
    }

    @Autowired
    private RecordingMailer mailer;

    /** Only the link that was used should ever have worked; the other would for an hour. */
    @Test
    @SuppressWarnings("rawtypes")
    void onceThePasswordIsResetTheOtherPendingLinkNoLongerWorks() {
        String email = "student-" + UUID.randomUUID() + "@example.com";
        rest.postForEntity("/api/v1/auth/register",
                Map.of("email", email, "password", "a-valid-password"), String.class);
        rest.postForEntity("/api/v1/auth/forgot-password", Map.of("email", email), String.class);
        String older = mailer.lastResetToken();
        rest.postForEntity("/api/v1/auth/forgot-password", Map.of("email", email), String.class);
        String newer = mailer.lastResetToken();

        ResponseEntity<String> used = rest.postForEntity("/api/v1/auth/reset-password",
                Map.of("token", newer, "newPassword", "another-valid-password"), String.class);
        ResponseEntity<Map> stale = rest.postForEntity("/api/v1/auth/reset-password",
                Map.of("token", older, "newPassword", "a-third-valid-password"), Map.class);

        assertThat(used.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(stale.getBody()).containsEntry("code", "AUTH_INVALID_TOKEN");
    }
}
