package com.tennisplatform.identity;

import com.tennisplatform.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
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

    /**
     * The javadoc of {@code POST /auth/refresh} promises that a missing session and a dead one
     * are indistinguishable. That was true of the status and false of the body until the error
     * contract was fixed: one path answered with a Problem Detail carrying no {@code code}, the
     * other with an empty 401.
     */
    @Test
    void aMissingSessionAndADeadOneAreIndistinguishable() {
        String email = uniqueEmail();
        register(email);
        verifyEmail(mailer.lastVerificationToken());
        login(email);

        String stolen = jar.get("refresh_token");
        refresh();
        jar.put("refresh_token", stolen);

        ResponseEntity<Map> dead = refresh();
        assertThat(dead.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(dead.getBody()).containsEntry("code", "AUTH_SESSION_EXPIRED");

        // Rejecting the cookie also clears it, so the next call is the "no session at all" path.
        assertThat(jar.has("refresh_token")).isFalse();
        ResponseEntity<Map> absent = refresh();

        assertThat(absent.getStatusCode()).isEqualTo(dead.getStatusCode());
        assertThat(absent.getBody()).isEqualTo(dead.getBody());
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

    /**
     * Found in the browser in Fase 11: every bearer request deleted the XSRF-TOKEN cookie, so
     * the next logout went out without it, was refused, and the session survived.
     */
    @Test
    @SuppressWarnings("unchecked")
    void usingTheApiDoesNotCostTheCsrfCookieSoLogoutStillWorks() {
        String email = uniqueEmail();
        register(email);
        String accessToken = (String) login(email).getBody().get("accessToken");
        String csrfBefore = jar.get("XSRF-TOKEN");

        HttpHeaders headers = jar.asHeaders();
        headers.setBearerAuth(accessToken);
        jar.absorb(rest.exchange("/api/v1/me", HttpMethod.GET, new HttpEntity<>(null, headers), Map.class));

        assertThat(jar.get("XSRF-TOKEN")).isEqualTo(csrfBefore);
        assertThat(logout().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(refresh().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** A student whose link expired asks for another, and the new one works (Fase 11). */
    @Test
    @SuppressWarnings("unchecked")
    void aSignedInStudentCanAskForANewVerificationLinkAndItWorks() {
        String email = uniqueEmail();
        register(email);
        String accessToken = (String) login(email).getBody().get("accessToken");

        ResponseEntity<String> resent = resendVerification(accessToken);

        assertThat(resent.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(mailer.verificationCount()).isEqualTo(2);
        assertThat(verifyEmail(mailer.lastVerificationToken()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get("/api/v1/me", accessToken).getBody()).containsEntry("status", "ACTIVE");
    }

    @Test
    void aVerifiedAddressGetsNoNewLink() {
        String email = uniqueEmail();
        register(email);
        verifyEmail(mailer.lastVerificationToken());
        String accessToken = (String) login(email).getBody().get("accessToken");

        assertThat(resendVerification(accessToken).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(mailer.verificationCount()).isEqualTo(1);
    }

    /** Asking by address would let anyone flood someone else's inbox: only the account holder may ask. */
    @Test
    void askingForANewLinkNeedsToBeSignedIn() {
        ResponseEntity<String> anonymous = rest.postForEntity("/api/v1/auth/verification-email",
                json(Map.of()), String.class);

        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aRegistrationThatBreaksTheRulesIsABadRequestAndCreatesNothing() {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/register",
                json(Map.of("email", "not-an-email", "password", "short")), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "VALIDATION_ERROR");
        assertThat(countUsers("not-an-email")).isZero();
    }

    /** A verification link works once: the second click is told the link is spent. */
    @Test
    void aVerificationLinkThatWasAlreadyUsedIsAConflict() {
        register(uniqueEmail());
        String token = mailer.lastVerificationToken();
        verifyEmail(token);

        ResponseEntity<Map> again = rest.postForEntity("/api/v1/auth/verify-email",
                json(Map.of("token", token)), Map.class);

        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(again.getBody()).containsEntry("code", "AUTH_INVALID_TOKEN");
    }

    @Test
    void verifyingWithoutATokenIsABadRequest() {
        assertThat(verifyEmail("").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aResetLinkThatIsNotRealIsAConflictAndAShortPasswordIsABadRequest() {
        ResponseEntity<Map> unknown = rest.postForEntity("/api/v1/auth/reset-password",
                json(Map.of("token", "not-a-real-token", "newPassword", "another-valid-password")), Map.class);
        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(unknown.getBody()).containsEntry("code", "AUTH_INVALID_TOKEN");

        ResponseEntity<Map> tooShort = rest.postForEntity("/api/v1/auth/reset-password",
                json(Map.of("token", "not-a-real-token", "newPassword", "short")), Map.class);
        assertThat(tooShort.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * The request only checks the length in characters; bcrypt ignores everything past 72
     * bytes, so a longer password would be accepted and silently cut (Fase 14).
     */
    @Test
    void aPasswordLongerThanBcryptCanHashIsRejectedWithItsOwnCode() {
        String email = uniqueEmail();

        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/register",
                json(Map.of("email", email, "password", "x".repeat(100))), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "AUTH_WEAK_PASSWORD");
        assertThat(countUsers(email)).isZero();
    }

    @Test
    void aVerificationLinkSentBeforeTheAccountWasDisabledNoLongerWorks() {
        String email = uniqueEmail();
        register(email);
        disable(email);

        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/verify-email",
                json(Map.of("token", mailer.lastVerificationToken())), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("code", "AUTH_ACCOUNT_NOT_ACTIVE");
    }

    @Test
    void aResetLinkSentBeforeTheAccountWasDisabledCannotChangeThePassword() {
        String email = uniqueEmail();
        register(email);
        verifyEmail(mailer.lastVerificationToken());
        rest.postForEntity("/api/v1/auth/forgot-password", json(Map.of("email", email)), String.class);
        disable(email);

        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/reset-password",
                json(Map.of("token", mailer.lastResetToken(), "newPassword", "another-valid-password")),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("code", "AUTH_ACCOUNT_NOT_ACTIVE");
    }

    /** Same 202 as for anyone, so the address is not revealed, but no link goes out. */
    @Test
    void aDisabledAccountIsSentNoResetLink() {
        String email = uniqueEmail();
        register(email);
        disable(email);

        ResponseEntity<String> response = rest.postForEntity("/api/v1/auth/forgot-password",
                json(Map.of("email", email)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(mailer.resetCount()).isZero();
    }

    /**
     * Otherwise anyone could send a stranger one reset email per request. The answer does not
     * change, so the cooldown reveals nothing about the address (26-fase15-analisis-seguridad.md).
     */
    @Test
    void aSecondResetEmailWithinFiveMinutesIsNotSentAndTheAnswerIsTheSame() {
        String email = uniqueEmail();
        register(email);

        ResponseEntity<String> first = rest.postForEntity("/api/v1/auth/forgot-password",
                json(Map.of("email", email)), String.class);
        ResponseEntity<String> second = rest.postForEntity("/api/v1/auth/forgot-password",
                json(Map.of("email", email)), String.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(mailer.resetCount()).isEqualTo(1);
    }

    /** Registering with the victim's address and pressing "resend" must not flood their inbox. */
    @Test
    void aSecondResendWithinFiveMinutesIsNotSent() {
        String email = uniqueEmail();
        register(email);
        String accessToken = (String) login(email).getBody().get("accessToken");

        assertThat(resendVerification(accessToken).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(resendVerification(accessToken).getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        assertThat(mailer.verificationCount()).isEqualTo(2);
    }

    @Test
    void registeringAgainAndAgainWarnsTheOwnerOnce() {
        String email = uniqueEmail();
        register(email);

        register(email);
        register(email);

        assertThat(mailer.existingAccountWarningCount()).isEqualTo(1);
    }

    // --- helpers ---------------------------------------------------------------

    private void disable(String email) {
        jdbc.update("UPDATE users SET status = 'DISABLED' WHERE email = ?", email);
    }

    private ResponseEntity<String> resendVerification(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return rest.exchange("/api/v1/auth/verification-email", HttpMethod.POST,
                new HttpEntity<>(null, headers), String.class);
    }

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
