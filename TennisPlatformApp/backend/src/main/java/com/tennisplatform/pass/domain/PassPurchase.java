package com.tennisplatform.pass.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * A pass a student is buying in the app (42-fase27.5-analisis-pago-online.md).
 *
 * <p>It copies the type when the purchase starts, like a recorded pass does: what the student
 * pays is what the type said then, and the pass that comes out of it is what they paid for, even
 * if the teacher changes the price or retires the type before the gateway confirms (decision 3).
 * The amount is set here, never by the browser.
 *
 * <p>{@code checkoutSessionId} is the gateway's reference for the payment. It is unique, which is
 * what turns a repeated notice into a no-op.
 */
public record PassPurchase(UUID id, UUID teacherUserId, UUID studentUserId, UUID passTypeId, String name,
                           PassLessonType lessonType, int lessons, int priceCents, Integer validityMonths,
                           PurchaseStatus status, String checkoutSessionId, String checkoutUrl, UUID passId,
                           Instant createdAt, Instant expiresAt, Instant paidAt) {

    /** Every price is in euros. */
    public static final String CURRENCY = "eur";

    /** The least the gateway charges: 0,50 euros. */
    public static final int MIN_ONLINE_PRICE_CENTS = 50;

    /** A student starts buying a pass of {@code type}. Retired types are no longer sold. */
    public static PassPurchase start(PassType type, UUID studentUserId, Instant now, Instant expiresAt) {
        if (type.retired()) {
            throw new PassTypeRetiredException("This pass type is no longer sold");
        }
        if (type.priceCents() < MIN_ONLINE_PRICE_CENTS) {
            throw new PriceTooLowToPayOnlineException("A pass under 0,50 euros cannot be paid online");
        }
        return new PassPurchase(null, type.teacherUserId(), studentUserId, type.id(), type.name(), type.lessonType(),
                type.lessons(), type.priceCents(), type.validityMonths(), PurchaseStatus.PENDING, null, null, null,
                now, expiresAt, null);
    }

    /** The gateway's page where the student pays. */
    public PassPurchase withCheckout(String sessionId, String url) {
        return new PassPurchase(id, teacherUserId, studentUserId, passTypeId, name, lessonType, lessons, priceCents,
                validityMonths, status, sessionId, url, passId, createdAt, expiresAt, paidAt);
    }

    /** Whether the gateway charged what this purchase asked for, no more and no less. */
    public boolean charged(long amountCents, String currency) {
        return amountCents == priceCents && currency != null && CURRENCY.equals(currency.toLowerCase(Locale.ROOT));
    }

    /**
     * Paid, with the pass it became. From {@code EXPIRED} too: the gateway's word that the money
     * arrived wins over an earlier notice that the page was left.
     */
    public PassPurchase paid(UUID newPassId, Instant now) {
        return new PassPurchase(id, teacherUserId, studentUserId, passTypeId, name, lessonType, lessons, priceCents,
                validityMonths, PurchaseStatus.PAID, checkoutSessionId, checkoutUrl, newPassId, createdAt, expiresAt,
                now);
    }

    /** Abandoned. A purchase already paid stays paid. */
    public PassPurchase expired() {
        if (status != PurchaseStatus.PENDING) {
            return this;
        }
        return new PassPurchase(id, teacherUserId, studentUserId, passTypeId, name, lessonType, lessons, priceCents,
                validityMonths, PurchaseStatus.EXPIRED, checkoutSessionId, checkoutUrl, passId, createdAt, expiresAt,
                paidAt);
    }

    public boolean isPaid() {
        return status == PurchaseStatus.PAID;
    }
}
