package com.tennisplatform.identity;

import com.tennisplatform.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end checks of the acceptance criteria for Fase 5, over real HTTP against a real
 * PostgreSQL.
 */
@Import(AuthFlowIntegrationTest.MailerStub.class)
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-valid-password";

    @TestConfiguration
    static class MailerStub {

        @Bean
        @Primary
        RecordingMailer recordingMailer() {
            return new RecordingMailer();
        }
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private RecordingMailer mailer;

    @Autowired
    private JdbcTemplate jdbc;

    private CookieJar jar;

    @BeforeEach
    void setUp() {
        mailer.clear();
        jar = new CookieJar();
    }

    /** Criterion 1: the whole happy path, demonstrated in one run. */
    @Test
    void aStudentCanRegisterVerifyLoginRefreshAndLogOut() {
        String email = uniqueEmail();

        assertThat(register(email).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(verifyEmail(mailer.lastVerificationToken()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> login = login(email);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).containsKey("accessToken");
        assertThat(((Map<?, ?>) login.getBody().get("user")).get("status")).isEqualTo("ACTIVE");

        // The refresh token must arrive as a cookie and never in the body.
        assertThat(jar.has("refresh_token")).isTrue();
        assertThat(login.getBody()).doesNotContainKey("refreshToken");

        String accessToken = (String) login.getBody().get("accessToken");
        ResponseEntity<Map> me = get("/api/v1/me", accessToken);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().get("email")).isEqualTo(email);

        String firstRefreshToken = jar.get("refresh_token");
        ResponseEntity<Map> refreshed = refresh();
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jar.get("refresh_token")).isNotEqualTo(firstRefreshToken);

        assertThat(logout().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    /**
     * Criterion 3: registering an address that already exists is indistinguishable from
     * registering a new one - same status, same empty body, no new account.
     */
    @Test
    void registeringAnExistingEmailIsIndistinguishableFromANewOne() {
        String email = uniqueEmail();

        ResponseEntity<String> first = register(email);
        ResponseEntity<String> second = register(email);

        assertThat(second.getStatusCode()).isEqualTo(first.getStatusCode());
        assertThat(second.getBody()).isEqualTo(first.getBody());
        assertThat(countUsers(email)).isEqualTo(1);

        // Only the first attempt produced a verification email; the second warned the owner.
        assertThat(mailer.verificationCount()).isEqualTo(1);
        assertThat(mailer.existingAccountWarningCount()).isEqualTo(1);
    }

    /** Criterion 2: presenting an already rotated refresh token kills the entire family. */
    @Test
    void reusingARotatedRefreshTokenRevokesTheWholeFamily() {
        String email = uniqueEmail();
        register(email);
        verifyEmail(mailer.lastVerificationToken());
        login(email);

        String stolen = jar.get("refresh_token");
        assertThat(refresh().getStatusCode()).isEqualTo(HttpStatus.OK);
        String rotated = jar.get("refresh_token");

        // The attacker replays the old cookie.
        jar.put("refresh_token", stolen);
        assertThat(refresh().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // And the legitimate successor is dead too: the family was revoked.
        jar.put("refresh_token", rotated);
        assertThat(refresh().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        Integer active = jdbc.queryForObject(
                "SELECT count(*) FROM refresh_tokens rt JOIN users u ON u.id = rt.user_id "
                        + "WHERE u.email = ? AND rt.revoked_at IS NULL", Integer.class, email);
        assertThat(active).isZero();
    }

    /** Criterion 4: the schema itself refuses a second teacher. */
    @Test
    void theSchemaAllowsOnlyOneTeacher() {
        jdbc.update("INSERT INTO users (email, password_hash, role, status) VALUES (?, ?, 'TEACHER', 'ACTIVE')",
                uniqueEmail(), "hash");

        String secondEmail = uniqueEmail();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        jdbc.update("INSERT INTO users (email, password_hash, role, status) "
                                + "VALUES (?, ?, 'TEACHER', 'ACTIVE')", secondEmail, "hash"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void anUnverifiedAccountCanSignInButIsReportedAsPending() {
        String email = uniqueEmail();
        register(email);

        ResponseEntity<Map> login = login(email);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) login.getBody().get("user")).get("status"))
                .isEqualTo("PENDING_VERIFICATION");
    }

    @Test
    void wrongCredentialsAndUnknownAccountsLookTheSame() {
        String email = uniqueEmail();
        register(email);

        ResponseEntity<String> wrongPassword = rest.postForEntity("/api/v1/auth/login",
                json(Map.of("email", email, "password", "the-wrong-password")), String.class);
        ResponseEntity<String> unknownEmail = rest.postForEntity("/api/v1/auth/login",
                json(Map.of("email", uniqueEmail(), "password", PASSWORD)), String.class);

        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getStatusCode()).isEqualTo(wrongPassword.getStatusCode());
        assertThat(unknownEmail.getBody()).isEqualTo(wrongPassword.getBody());
    }

    @Test
    void anUnverifiedTokenIsRejectedAndTheEndpointStaysClosed() {
        assertThat(get("/api/v1/me", "not-a-real-token").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rest.getForEntity("/api/v1/me", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void resettingThePasswordRevokesExistingSessions() {
        String email = uniqueEmail();
        register(email);
        verifyEmail(mailer.lastVerificationToken());
        login(email);

        rest.postForEntity("/api/v1/auth/forgot-password", json(Map.of("email", email)), String.class);
        ResponseEntity<String> reset = rest.postForEntity("/api/v1/auth/reset-password",
                json(Map.of("token", mailer.lastResetToken(), "newPassword", "another-valid-password")),
                String.class);

        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(refresh().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** Unknown addresses must not be distinguishable here either. */
    @Test
    void forgotPasswordAlwaysAccepts() {
        assertThat(rest.postForEntity("/api/v1/auth/forgot-password",
                json(Map.of("email", uniqueEmail())), String.class).getStatusCode())
                .isEqualTo(HttpStatus.ACCEPTED);
    }

    // --- helpers ---------------------------------------------------------------

    private String uniqueEmail() {
        return "student-" + java.util.UUID.randomUUID() + "@example.com";
    }

    private ResponseEntity<String> register(String email) {
        ResponseEntity<String> response = rest.postForEntity("/api/v1/auth/register",
                json(Map.of("email", email, "password", PASSWORD)), String.class);
        jar.absorb(response);
        return response;
    }

    private ResponseEntity<String> verifyEmail(String token) {
        return rest.postForEntity("/api/v1/auth/verify-email", json(Map.of("token", token)), String.class);
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> login(String email) {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login",
                json(Map.of("email", email, "password", PASSWORD)), Map.class);
        jar.absorb(response);
        return response;
    }

    private ResponseEntity<Map> refresh() {
        ResponseEntity<Map> response = rest.exchange("/api/v1/auth/refresh", HttpMethod.POST,
                new HttpEntity<>(null, withCsrf()), Map.class);
        jar.absorb(response);
        return response;
    }

    private ResponseEntity<String> logout() {
        ResponseEntity<String> response = rest.exchange("/api/v1/auth/logout", HttpMethod.POST,
                new HttpEntity<>(null, withCsrf()), String.class);
        jar.absorb(response);
        return response;
    }

    private ResponseEntity<Map> get(String path, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(null, headers), Map.class);
    }

    /**
     * Double submit: the same value travels as a cookie and as a header. A cross-site form can
     * make the browser send the cookie, but cannot read it to set the header.
     */
    private HttpHeaders withCsrf() {
        HttpHeaders headers = jar.asHeaders();
        String csrf = jar.get("XSRF-TOKEN");
        if (csrf != null) {
            headers.add("X-XSRF-TOKEN", csrf);
        }
        return headers;
    }

    private HttpEntity<Map<String, String>> json(Map<String, String> body) {
        HttpHeaders headers = jar.asHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private Integer countUsers(String email) {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email);
    }
}
