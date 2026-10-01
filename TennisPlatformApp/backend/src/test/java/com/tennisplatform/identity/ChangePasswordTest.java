package com.tennisplatform.identity;

import com.tennisplatform.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code POST /me/password} (30-fase19-analisis-cierre-mvp.md): the caller stays signed in on
 * this device, and every other session of the account ends.
 */
class ChangePasswordTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-valid-password";
    private static final String NEW_PASSWORD = "another-valid-password";

    private String email;

    @BeforeEach
    void register() {
        email = "student-" + UUID.randomUUID() + "@example.com";
        rest.postForEntity("/api/v1/auth/register", Map.of("email", email, "password", PASSWORD), String.class);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void changingThePasswordEndsTheOtherSessionsAndKeepsThisOne() {
        Device laptop = signIn(PASSWORD);
        Device phone = signIn(PASSWORD);

        ResponseEntity<Map> changed = phone.changePassword(PASSWORD, NEW_PASSWORD);

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody()).containsKey("accessToken").doesNotContainKey("refreshToken");
        assertThat(laptop.refresh().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(phone.refresh().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login(PASSWORD).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login(NEW_PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /** A stolen access token is not enough: without the current password nothing changes. */
    @Test
    @SuppressWarnings("rawtypes")
    void aWrongCurrentPasswordChangesNothing() {
        Device laptop = signIn(PASSWORD);
        Device phone = signIn(PASSWORD);

        ResponseEntity<Map> refused = phone.changePassword("not-" + PASSWORD, NEW_PASSWORD);

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(refused.getBody()).containsEntry("code", "CURRENT_PASSWORD_INCORRECT");
        assertThat(laptop.refresh().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login(PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void aShortNewPasswordIsRefused() {
        ResponseEntity<Map> refused = signIn(PASSWORD).changePassword(PASSWORD, "short");

        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(login(PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void withoutATokenItAnswersUnauthorized() {
        ResponseEntity<String> anonymous = rest.postForEntity("/api/v1/me/password",
                Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD), String.class);

        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> login(String password) {
        return rest.postForEntity("/api/v1/auth/login", Map.of("email", email, "password", password), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private Device signIn(String password) {
        ResponseEntity<Map> login = login(password);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        Device device = new Device(new CookieJar(), (String) login.getBody().get("accessToken"));
        device.jar().absorb(login);
        return device;
    }

    /** One browser: its cookies and its access token. */
    private final class Device {

        private final CookieJar jar;
        private String accessToken;

        private Device(CookieJar jar, String accessToken) {
            this.jar = jar;
            this.accessToken = accessToken;
        }

        CookieJar jar() {
            return jar;
        }

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> changePassword(String current, String next) {
            HttpHeaders headers = jar.asHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> response = rest.exchange("/api/v1/me/password", HttpMethod.POST,
                    new HttpEntity<>(Map.of("currentPassword", current, "newPassword", next), headers), Map.class);
            jar.absorb(response);
            if (response.getStatusCode().is2xxSuccessful()) {
                accessToken = (String) response.getBody().get("accessToken");
            }
            return response;
        }

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> refresh() {
            HttpHeaders headers = jar.asHeaders();
            String csrf = jar.get("XSRF-TOKEN");
            if (csrf != null) {
                headers.add("X-XSRF-TOKEN", csrf);
            }
            ResponseEntity<Map> response = rest.exchange("/api/v1/auth/refresh", HttpMethod.POST,
                    new HttpEntity<>(null, headers), Map.class);
            jar.absorb(response);
            return response;
        }
    }
}
