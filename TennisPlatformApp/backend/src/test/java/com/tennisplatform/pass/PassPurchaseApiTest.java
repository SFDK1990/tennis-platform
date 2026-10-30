package com.tennisplatform.pass;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Buying a pass in the app, with Stripe's keys set and Stripe played by {@link FakeStripe}
 * (42-fase27.5-analisis-pago-online.md). The notices are signed here the way Stripe signs them.
 */
class PassPurchaseApiTest extends AbstractPassTest {

    @DynamicPropertySource
    static void stripe(DynamicPropertyRegistry registry) {
        registry.add("tennis.pass.stripe-secret-key", () -> FakeStripe.SECRET_KEY);
        registry.add("tennis.pass.stripe-webhook-secret", () -> FakeStripe.WEBHOOK_SECRET);
        registry.add("tennis.pass.stripe-api-base-url", FakeStripe::baseUrl);
        registry.add("tennis.pass.frontend-base-url", () -> "https://tenis.example");
    }

    @AfterEach
    void stripeAnswersAgain() {
        FakeStripe.answerWith(200);
    }

    // --- Starting a purchase -------------------------------------------------------------------

    /** Decision 2: off until the teacher switches it on; with the keys set, it can be. */
    @Test
    @SuppressWarnings("rawtypes")
    void nothingIsSoldUntilTheTeacherSwitchesItOn() {
        Student student = aStudentWhoMayBook();
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        assertThat(types(student.token())).containsEntry("onlinePayment", false)
                .containsEntry("onlinePaymentAvailable", true);

        ResponseEntity<Map> off = buy(student.token(), type);
        assertThat(off.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(off.getBody()).containsEntry("code", "ONLINE_PAYMENT_DISABLED");

        ResponseEntity<Map> on = settings(Map.of("onlinePayment", true));
        assertThat(on.getBody()).containsEntry("onlinePayment", true).containsEntry("required", false)
                .containsEntry("onlinePaymentAvailable", true);
        assertThat(types(student.token())).containsEntry("onlinePayment", true);
        assertThat(buy(student.token(), type).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Each switch keeps its value when the other one changes.
        requirePasses(true);
        assertThat(types(student.token())).containsEntry("onlinePayment", true).containsEntry("required", true);
    }

    /** The amount comes from the type, never from the browser, and the purchase id travels with it. */
    @Test
    @SuppressWarnings("rawtypes")
    void opensStripesPageForWhatTheTypeCosts() {
        Student student = aStudentWhoMayBook();
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        sellOnline();

        ResponseEntity<Map> started = rest.exchange("/api/v1/me/pass-purchases", HttpMethod.POST,
                new HttpEntity<>(Map.of("passTypeId", type, "priceCents", 1), jsonBearer(student.token())), Map.class);

        assertThat(started.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map purchase = started.getBody();
        String id = (String) purchase.get("id");
        assertThat(purchase).containsEntry("status", "PENDING").containsEntry("priceCents", 10_000)
                .containsEntry("passId", null).containsEntry("name", "10 de grupo");
        assertThat((String) purchase.get("checkoutUrl")).startsWith("https://checkout.stripe.com/");

        Map<String, String> form = FakeStripe.lastForm();
        assertThat(form).containsEntry("mode", "payment")
                .containsEntry("line_items[0][price_data][unit_amount]", "10000")
                .containsEntry("line_items[0][price_data][currency]", "eur")
                .containsEntry("line_items[0][price_data][product_data][name]", "10 de grupo")
                .containsEntry("client_reference_id", id)
                .containsEntry("success_url", "https://tenis.example/passes?purchase=" + id)
                .containsEntry("cancel_url", "https://tenis.example/passes?purchase=" + id + "&cancelled=true");
        assertThat(FakeStripe.lastAuthorization()).isEqualTo("Bearer " + FakeStripe.SECRET_KEY);
        assertThat(FakeStripe.lastIdempotencyKey()).isEqualTo("purchase-" + id);
        assertThat(passOfPurchase(student)).isEmpty();
    }

    /** As to book: verified and managed. The other refusals are the type's. */
    @Test
    @SuppressWarnings("rawtypes")
    void onlyAVerifiedManagedStudentBuysAnExistingTypeThatIsStillSold() {
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        sellOnline();

        Student unverified = aStudentWithAProfile();
        manage(unverified);
        assertThat(buy(unverified.token(), type).getBody()).containsEntry("code", "EMAIL_NOT_VERIFIED");

        Student unmanaged = aStudentWithAProfile();
        verify(unmanaged);
        assertThat(buy(tokenOf(unmanaged.email(), PASSWORD), type).getBody()).containsEntry("code", "STUDENT_NOT_MANAGED");

        Student student = aStudentWhoMayBook();
        assertThat(buy(student.token(), UUID.randomUUID()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(buy(teacherToken, type).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        UUID cheap = aPassTypeCosting("Suelta", 49);
        ResponseEntity<Map> tooCheap = buy(student.token(), cheap);
        assertThat(tooCheap.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(tooCheap.getBody()).containsEntry("code", "PRICE_TOO_LOW_TO_PAY_ONLINE");

        retire(type);
        assertThat(buy(student.token(), type).getBody()).containsEntry("code", "PASS_TYPE_RETIRED");
        assertThat(purchasesInTheDatabase()).isZero();
    }

    /** Stripe down or refusing: a 503 the student can retry, and no purchase left behind. */
    @Test
    @SuppressWarnings("rawtypes")
    void whenStripeFailsTheStudentHearsSoAndNothingIsKept() {
        Student student = aStudentWhoMayBook();
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        sellOnline();
        FakeStripe.answerWith(500);

        ResponseEntity<Map> failed = buy(student.token(), type);

        assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(failed.getBody()).containsEntry("code", "PAYMENT_GATEWAY_UNAVAILABLE");
        assertThat(purchasesInTheDatabase()).isZero();
    }

    // --- Stripe's notice -----------------------------------------------------------------------

    /** Decision 3: the pass is born with the signed notice, paid online and from today. */
    @Test
    @SuppressWarnings("rawtypes")
    void aSignedPaymentNoticeCreatesThePass() {
        Student student = aStudentWhoMayBook();
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        sellOnline();
        Map purchase = buy(student.token(), type).getBody();

        assertThat(notify(completed(sessionOf(purchase), 10_000, "eur", "paid")).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        Map paid = purchase(student.token(), purchase.get("id"));
        assertThat(paid).containsEntry("status", "PAID").containsEntry("checkoutUrl", null);
        assertThat(paid.get("paidAt")).isNotNull();
        Map pass = passOf(student, UUID.fromString((String) paid.get("passId")));
        assertThat(pass).containsEntry("paymentMethod", "ONLINE").containsEntry("paidOn", today().toString())
                .containsEntry("validUntil", today().plusMonths(3).toString()).containsEntry("remaining", 10)
                .containsEntry("priceCents", 10_000).containsEntry("status", "ACTIVE");
    }

    /** The mutation "no check of the repeated notice" makes this test fail with two passes. */
    @Test
    @SuppressWarnings("rawtypes")
    void aRepeatedNoticeCreatesOnePass() {
        Student student = aStudentWhoMayBook();
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        sellOnline();
        Map purchase = buy(student.token(), type).getBody();
        String notice = completed(sessionOf(purchase), 10_000, "eur", "paid");

        notify(notice);
        notify(notice);
        notify(event("checkout.session.async_payment_succeeded", sessionOf(purchase), 10_000, "eur", "paid"));

        assertThat(passOfPurchase(student)).hasSize(1);
    }

    /** The mutation "no check of the signature" makes this test fail with a pass. */
    @Test
    @SuppressWarnings("rawtypes")
    void aNoticeWithoutAGoodRecentSignatureChangesNothing() {
        Student student = aStudentWhoMayBook();
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        sellOnline();
        Map purchase = buy(student.token(), type).getBody();
        String notice = completed(sessionOf(purchase), 10_000, "eur", "paid");
        long now = clock.instant().getEpochSecond();

        List<String> badSignatures = new java.util.ArrayList<>(List.of(
                "t=" + now + ",v1=" + hmac(now, notice, "whsec_somebody_else"),
                "t=" + (now - 600) + ",v1=" + hmac(now - 600, notice, FakeStripe.WEBHOOK_SECRET),
                "t=" + now));
        badSignatures.add(null);
        for (String signature : badSignatures) {
            ResponseEntity<Map> refused = notify(notice, signature);
            assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(refused.getBody()).containsEntry("code", "INVALID_WEBHOOK_SIGNATURE");
        }

        assertThat(purchase(student.token(), purchase.get("id"))).containsEntry("status", "PENDING");
        assertThat(passOfPurchase(student)).isEmpty();
    }

    /** The mutation "accept another amount" makes this test fail with a pass. */
    @Test
    @SuppressWarnings("rawtypes")
    void aPaymentOfAnotherAmountOrCurrencyCreatesNoPass() {
        Student student = aStudentWhoMayBook();
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        sellOnline();
        Map purchase = buy(student.token(), type).getBody();

        assertThat(notify(completed(sessionOf(purchase), 100, "eur", "paid")).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        notify(completed(sessionOf(purchase), 10_000, "usd", "paid"));

        assertThat(purchase(student.token(), purchase.get("id"))).containsEntry("status", "PENDING");
        assertThat(passOfPurchase(student)).isEmpty();
    }

    /** Decision 3: what was paid is what the pass gives, whatever the teacher did since. */
    @Test
    @SuppressWarnings("rawtypes")
    void aTypeRetiredOrRepricedAfterThePurchaseStartedStillGivesThePassThatWasPaid() {
        Student student = aStudentWhoMayBook();
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        sellOnline();
        Map purchase = buy(student.token(), type).getBody();
        rest.exchange("/api/v1/teacher/pass-types/" + type, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("priceCents", 20_000, "lessons", 5, "retired", true), jsonBearer(teacherToken)),
                Map.class);

        notify(completed(sessionOf(purchase), 10_000, "eur", "paid"));

        Map pass = passOf(student, UUID.fromString((String) purchase(student.token(), purchase.get("id")).get("passId")));
        assertThat(pass).containsEntry("priceCents", 10_000).containsEntry("lessons", 10);
    }

    /** A payment still on its way (some methods confirm later) waits; an abandoned page expires. */
    @Test
    @SuppressWarnings("rawtypes")
    void aPaymentOnItsWayWaitsAndAnAbandonedOneExpires() {
        Student student = aStudentWhoMayBook();
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        sellOnline();
        Map waiting = buy(student.token(), type).getBody();
        Map abandoned = buy(student.token(), type).getBody();

        notify(completed(sessionOf(waiting), 10_000, "eur", "unpaid"));
        assertThat(purchase(student.token(), waiting.get("id"))).containsEntry("status", "PENDING");
        notify(event("checkout.session.async_payment_succeeded", sessionOf(waiting), 10_000, "eur", "paid"));
        assertThat(purchase(student.token(), waiting.get("id"))).containsEntry("status", "PAID");

        notify(event("checkout.session.expired", sessionOf(abandoned), 10_000, "eur", "unpaid"));
        assertThat(purchase(student.token(), abandoned.get("id"))).containsEntry("status", "EXPIRED")
                .containsEntry("checkoutUrl", null).containsEntry("passId", null);
        // An expired notice after the payment leaves it paid.
        notify(event("checkout.session.expired", sessionOf(waiting), 10_000, "eur", "paid"));
        assertThat(purchase(student.token(), waiting.get("id"))).containsEntry("status", "PAID");
        assertThat(passOfPurchase(student)).hasSize(1);
    }

    /** Any other event, or one about a session the app does not know, is acknowledged and left alone. */
    @Test
    void aNoticeTheAppDoesNotNeedIsAcknowledged() {
        assertThat(notify(completed("cs_test_unknown", 10_000, "eur", "paid")).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(notify(event("charge.refunded", "ch_1", 10_000, "eur", "paid")).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(notify("not json").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /** A type somebody started buying is in use: their payment may still arrive. It can be retired. */
    @Test
    @SuppressWarnings("rawtypes")
    void aTypeSomebodyStartedBuyingIsNotDeleted() {
        Student student = aStudentWhoMayBook();
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        sellOnline();
        buy(student.token(), type);

        ResponseEntity<Map> deleted = rest.exchange("/api/v1/teacher/pass-types/" + type, HttpMethod.DELETE,
                new HttpEntity<>(bearer(teacherToken)), Map.class);

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(deleted.getBody()).containsEntry("code", "PASS_TYPE_IN_USE");
    }

    // --- Reading it back -----------------------------------------------------------------------

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void aPurchaseIsOnlyItsStudentsAndGoesInTheirExport() {
        Student student = aStudentWhoMayBook();
        Student other = aStudentWhoMayBook();
        UUID type = aPassType("10 de grupo", "GROUP", 10, 3);
        sellOnline();
        Map purchase = buy(student.token(), type).getBody();
        String path = "/api/v1/me/pass-purchases/" + purchase.get("id");

        assertThat(rest.exchange(path, HttpMethod.GET, new HttpEntity<>(bearer(other.token())), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rest.exchange(path, HttpMethod.GET, new HttpEntity<>(bearer(teacherToken)), Map.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(rest.exchange("/api/v1/me/pass-purchases/" + UUID.randomUUID(), HttpMethod.GET,
                new HttpEntity<>(bearer(student.token())), Map.class).getBody()).containsEntry("code", "PURCHASE_NOT_FOUND");

        Map export = rest.exchange("/api/v1/me/export", HttpMethod.GET, new HttpEntity<>(bearer(student.token())),
                Map.class).getBody();
        assertThat((List<Map>) export.get("passPurchases")).extracting(p -> p.get("id")).containsExactly(purchase.get("id"));
    }

    // --- helpers --------------------------------------------------------------------------------

    private void sellOnline() {
        assertThat(settings(Map.of("onlinePayment", true)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> settings(Map<String, Object> body) {
        return rest.exchange("/api/v1/teacher/pass-settings", HttpMethod.PUT,
                new HttpEntity<>(body, jsonBearer(teacherToken)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private Map types(String token) {
        return rest.exchange("/api/v1/pass-types", HttpMethod.GET, new HttpEntity<>(bearer(token)), Map.class).getBody();
    }

    @SuppressWarnings("rawtypes")
    private UUID aPassTypeCosting(String name, int priceCents) {
        ResponseEntity<Map> created = rest.exchange("/api/v1/teacher/pass-types", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name, "lessonType", "GROUP", "lessons", 1, "priceCents", priceCents),
                        jsonBearer(teacherToken)), Map.class);
        return UUID.fromString((String) created.getBody().get("id"));
    }

    private void retire(UUID type) {
        rest.exchange("/api/v1/teacher/pass-types/" + type, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("retired", true), jsonBearer(teacherToken)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> buy(String token, UUID type) {
        return rest.exchange("/api/v1/me/pass-purchases", HttpMethod.POST,
                new HttpEntity<>(Map.of("passTypeId", type), jsonBearer(token)), Map.class);
    }

    @SuppressWarnings("rawtypes")
    private Map purchase(String token, Object id) {
        return rest.exchange("/api/v1/me/pass-purchases/" + id, HttpMethod.GET, new HttpEntity<>(bearer(token)),
                Map.class).getBody();
    }

    /** The passes the student has; this test class only creates them by buying. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Map> passOfPurchase(Student student) {
        return (List<Map>) rest.exchange("/api/v1/me/passes", HttpMethod.GET, new HttpEntity<>(bearer(student.token())),
                Map.class).getBody().get("items");
    }

    private int purchasesInTheDatabase() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM pass_purchases", Integer.class);
        return count == null ? 0 : count;
    }

    @SuppressWarnings("rawtypes")
    private String sessionOf(Map purchase) {
        return jdbc.queryForObject("SELECT checkout_session_id FROM pass_purchases WHERE id = ?", String.class,
                UUID.fromString((String) purchase.get("id")));
    }

    private static String completed(String sessionId, long amount, String currency, String paymentStatus) {
        return event("checkout.session.completed", sessionId, amount, currency, paymentStatus);
    }

    private static String event(String type, String sessionId, long amount, String currency, String paymentStatus) {
        return """
                {"id":"evt_%s","object":"event","type":"%s","data":{"object":{"id":"%s","object":"checkout.session",\
                "amount_total":%d,"currency":"%s","payment_status":"%s"}}}""".formatted(UUID.randomUUID(), type,
                sessionId, amount, currency, paymentStatus);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> notify(String body) {
        long now = clock.instant().getEpochSecond();
        return notify(body, "t=" + now + ",v1=" + hmac(now, body, FakeStripe.WEBHOOK_SECRET));
    }

    /** Anonymous, as Stripe calls. */
    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> notify(String body, String signature) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (signature != null) {
            headers.set("Stripe-Signature", signature);
        }
        return rest.exchange("/api/v1/payments/stripe/webhook", HttpMethod.POST,
                new HttpEntity<>(body.getBytes(StandardCharsets.UTF_8), headers), Map.class);
    }

    private static String hmac(long timestamp, String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
