package com.tennisplatform.pass.adapters.in.web;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Checks the {@code Stripe-Signature} header of a notice: {@code t=<seconds>,v1=<hex>[,v1=...]},
 * where each {@code v1} is HMAC-SHA256 of {@code "<t>.<body>"} with the webhook's secret. The
 * body is taken as the exact bytes received; re-serialising it would change the signature.
 *
 * <p>A timestamp more than five minutes away is refused, so a notice captured once cannot be
 * replayed later. Comparison is in constant time.
 */
final class StripeSignature {

    static final Duration TOLERANCE = Duration.ofMinutes(5);

    private StripeSignature() {
    }

    static boolean valid(byte[] payload, String header, String secret, Instant now) {
        if (header == null || secret == null || secret.isBlank()) {
            return false;
        }
        Long timestamp = null;
        List<String> signatures = new ArrayList<>();
        for (String part : header.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if (pair[0].equals("t")) {
                timestamp = parse(pair[1]);
            } else if (pair[0].equals("v1")) {
                signatures.add(pair[1]);
            }
        }
        if (timestamp == null || signatures.isEmpty()
                || Duration.between(Instant.ofEpochSecond(timestamp), now).abs().compareTo(TOLERANCE) > 0) {
            return false;
        }
        byte[] expected = sign(timestamp, payload, secret).getBytes(StandardCharsets.US_ASCII);
        boolean matches = false;
        for (String signature : signatures) {
            matches |= MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.US_ASCII));
        }
        return matches;
    }

    /** What Stripe puts in {@code v1}; the tests sign their notices with it too. */
    static String sign(long timestamp, byte[] payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(mac.doFinal(payload));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is part of every JDK", e);
        }
    }

    private static Long parse(String seconds) {
        try {
            return Long.parseLong(seconds);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
