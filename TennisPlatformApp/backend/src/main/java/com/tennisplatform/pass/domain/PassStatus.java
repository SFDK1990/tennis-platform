package com.tennisplatform.pass.domain;

/** Derived when reading, never stored: it depends on the day and on the bookings. */
public enum PassStatus {
    ACTIVE,
    USED_UP,
    EXPIRED,
    VOIDED
}
