package com.tennisplatform.membership.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * What the teacher recorded for one month of a membership: paid, with the amount, the day and how,
 * or not charged (a holiday, an injury). A fee recorded by mistake is voided, and then the month
 * can be recorded again; only one live fee per membership and month.
 */
public record MonthlyFee(UUID id, UUID membershipId, YearMonth month, FeeKind kind, int amountCents,
                         LocalDate paidOn, FeePaymentMethod paymentMethod, Instant recordedAt, Instant voidedAt) {

    public static MonthlyFee paid(UUID membershipId, YearMonth month, int amountCents, LocalDate paidOn,
                                  FeePaymentMethod method, Instant now) {
        if (month == null) {
            throw new InvalidMembershipException("Say which month was paid");
        }
        if (amountCents < 0 || amountCents > FixedGroup.MAX_FEE_CENTS) {
            throw new InvalidMembershipException("The amount goes from 0 to " + FixedGroup.MAX_FEE_CENTS + " cents");
        }
        if (paidOn == null) {
            throw new InvalidMembershipException("Say when the month was paid");
        }
        if (method == null) {
            throw new InvalidMembershipException("Say how the month was paid: CASH, BIZUM or TRANSFER");
        }
        return new MonthlyFee(null, membershipId, month, FeeKind.PAID, amountCents, paidOn, method, now, null);
    }

    public static MonthlyFee waived(UUID membershipId, YearMonth month, Instant now) {
        if (month == null) {
            throw new InvalidMembershipException("Say which month is not charged");
        }
        return new MonthlyFee(null, membershipId, month, FeeKind.WAIVED, 0, null, null, now, null);
    }

    public MonthlyFee voided(Instant now) {
        if (voidedAt != null) {
            return this;
        }
        return new MonthlyFee(id, membershipId, month, kind, amountCents, paidOn, paymentMethod, recordedAt, now);
    }

    public boolean isVoided() {
        return voidedAt != null;
    }
}
