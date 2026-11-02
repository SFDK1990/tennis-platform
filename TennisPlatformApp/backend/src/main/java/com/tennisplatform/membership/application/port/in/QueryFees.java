package com.tennisplatform.membership.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

/** What other modules read about the fees: the month's summary adds what was collected. */
public interface QueryFees {

    /** The fees paid on a day in {@code [from, until)}, voided ones left out, and what they add up to. */
    FeeIncome collectedBetween(UUID teacherUserId, LocalDate from, LocalDate until);

    record FeeIncome(int count, long totalCents) {
    }
}
