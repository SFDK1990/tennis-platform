package com.tennisplatform.pass.adapters.in.web;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The signature of Stripe's notices, computed here independently of the code under test, the way
 * Stripe documents it: HMAC-SHA256 of {@code "<t>.<body>"} with the webhook's secret, in hex.
 */
class StripeSignatureTest {

    private static final String SECRET = "whsec_test_secret";
    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
    private static final byte[] BODY = "{\"type\":\"checkout.session.completed\"}".getBytes(StandardCharsets.UTF_8);

    private static String hmac(long timestamp, byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test
    void acceptsANoticeSignedWithTheSecretAndRecent() throws Exception {
        long t = NOW.getEpochSecond();
        assertThat(StripeSignature.valid(BODY, "t=" + t + ",v1=" + hmac(t, BODY, SECRET), SECRET, NOW)).isTrue();
        assertThat(StripeSignature.sign(t, BODY, SECRET)).isEqualTo(hmac(t, BODY, SECRET));
    }

    /** While Stripe rolls the secret it signs with both; either one is enough. */
    @Test
    void acceptsAnyOfSeveralSignaturesAndIgnoresOtherSchemes() throws Exception {
        long t = NOW.getEpochSecond();
        String header = "t=" + t + ",v0=abc,v1=" + hmac(t, BODY, "whsec_old") + ",v1=" + hmac(t, BODY, SECRET);
        assertThat(StripeSignature.valid(BODY, header, SECRET, NOW)).isTrue();
    }

    @Test
    void refusesAnotherSecretAChangedBodyOrAMissingPart() throws Exception {
        long t = NOW.getEpochSecond();
        String signed = "t=" + t + ",v1=" + hmac(t, BODY, SECRET);

        assertThat(StripeSignature.valid(BODY, "t=" + t + ",v1=" + hmac(t, BODY, "whsec_other"), SECRET, NOW)).isFalse();
        assertThat(StripeSignature.valid("{\"type\":\"other\"}".getBytes(StandardCharsets.UTF_8), signed, SECRET, NOW))
                .isFalse();
        assertThat(StripeSignature.valid(BODY, null, SECRET, NOW)).isFalse();
        assertThat(StripeSignature.valid(BODY, "v1=" + hmac(t, BODY, SECRET), SECRET, NOW)).isFalse();
        assertThat(StripeSignature.valid(BODY, "t=" + t, SECRET, NOW)).isFalse();
        assertThat(StripeSignature.valid(BODY, "t=soon,v1=" + hmac(t, BODY, SECRET), SECRET, NOW)).isFalse();
        assertThat(StripeSignature.valid(BODY, "garbage", SECRET, NOW)).isFalse();
    }

    /** Without a secret in the environment nothing can be checked, so nothing is accepted. */
    @Test
    void refusesEverythingWithoutASecret() throws Exception {
        long t = NOW.getEpochSecond();
        String signed = "t=" + t + ",v1=" + hmac(t, BODY, SECRET);
        assertThat(StripeSignature.valid(BODY, signed, "", NOW)).isFalse();
        assertThat(StripeSignature.valid(BODY, signed, null, NOW)).isFalse();
    }

    /** A notice captured once cannot be replayed later, nor one dated in the future accepted. */
    @Test
    void refusesATimestampMoreThanFiveMinutesAway() throws Exception {
        long old = NOW.minusSeconds(301).getEpochSecond();
        long edge = NOW.minusSeconds(300).getEpochSecond();
        long future = NOW.plusSeconds(301).getEpochSecond();

        assertThat(StripeSignature.valid(BODY, "t=" + old + ",v1=" + hmac(old, BODY, SECRET), SECRET, NOW)).isFalse();
        assertThat(StripeSignature.valid(BODY, "t=" + edge + ",v1=" + hmac(edge, BODY, SECRET), SECRET, NOW)).isTrue();
        assertThat(StripeSignature.valid(BODY, "t=" + future + ",v1=" + hmac(future, BODY, SECRET), SECRET, NOW))
                .isFalse();
    }
}
