package com.tennisplatform.membership.domain;

/**
 * How a month of a membership stands. Only PAID and WAIVED are recorded; PENDING and OVERDUE are
 * worked out when read, from the due day and the date, so nothing has to run for a fee to fall due.
 */
public enum FeeStatus {
    PAID,
    WAIVED,
    PENDING,
    OVERDUE
}
