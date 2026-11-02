package com.tennisplatform.membership.domain;

/** What the teacher recorded for a month: that it was paid, or that it is not charged. */
public enum FeeKind {
    PAID,
    WAIVED;

    public static FeeKind parse(String value) {
        if (value == null) {
            throw new InvalidMembershipException("Say whether the month was PAID or WAIVED");
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidMembershipException("A month is PAID or WAIVED");
        }
    }
}
