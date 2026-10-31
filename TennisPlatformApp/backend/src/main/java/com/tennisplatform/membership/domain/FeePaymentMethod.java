package com.tennisplatform.membership.domain;

/** How a fee was paid, outside the app: the fee is recorded, never charged (01-analisis-funcional.md §16). */
public enum FeePaymentMethod {
    CASH,
    BIZUM,
    TRANSFER;

    /** The wire value, or an {@link InvalidMembershipException} that names the ones accepted. */
    public static FeePaymentMethod parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidMembershipException("A fee is paid with CASH, BIZUM or TRANSFER");
        }
    }
}
