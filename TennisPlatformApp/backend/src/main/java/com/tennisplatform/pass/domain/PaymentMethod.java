package com.tennisplatform.pass.domain;

import java.util.Locale;

/**
 * How the student paid. The first three the teacher writes down, for a payment made outside the
 * app (Fase 27, decision 3); {@code ONLINE} the app sets itself when the payment gateway confirms
 * a purchase (Fase 27.5), so the teacher cannot choose it.
 */
public enum PaymentMethod {
    CASH,
    BIZUM,
    TRANSFER,
    ONLINE;

    /** A method the teacher records by hand. */
    public static PaymentMethod parse(String value) {
        if (value == null) {
            throw new InvalidPassException("Say how the pass was paid: CASH, BIZUM or TRANSFER");
        }
        PaymentMethod method;
        try {
            method = valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidPassException("Unknown payment method: " + value + ". Expected CASH, BIZUM or TRANSFER");
        }
        if (method == ONLINE) {
            throw new InvalidPassException("ONLINE is set by the app when a payment is confirmed. Expected CASH, "
                    + "BIZUM or TRANSFER");
        }
        return method;
    }
}
