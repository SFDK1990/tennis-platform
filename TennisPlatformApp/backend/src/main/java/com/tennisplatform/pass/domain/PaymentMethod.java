package com.tennisplatform.pass.domain;

import java.util.Locale;

/** How the student paid the teacher, outside the app (decision 3). */
public enum PaymentMethod {
    CASH,
    BIZUM,
    TRANSFER;

    public static PaymentMethod parse(String value) {
        if (value == null) {
            throw new InvalidPassException("Say how the pass was paid: CASH, BIZUM or TRANSFER");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidPassException("Unknown payment method: " + value + ". Expected CASH, BIZUM or TRANSFER");
        }
    }
}
