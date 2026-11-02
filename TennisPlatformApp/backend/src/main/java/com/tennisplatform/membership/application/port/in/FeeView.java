package com.tennisplatform.membership.application.port.in;

import com.tennisplatform.membership.domain.MonthlyFee;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A fee the teacher recorded, with its wire values. */
public record FeeView(UUID id, UUID membershipId, String month, String kind, int amountCents, LocalDate paidOn,
                      String paymentMethod, Instant recordedAt, Instant voidedAt) {

    public static FeeView from(MonthlyFee fee) {
        return new FeeView(fee.id(), fee.membershipId(), fee.month().toString(), fee.kind().name(), fee.amountCents(),
                fee.paidOn(), fee.paymentMethod() == null ? null : fee.paymentMethod().name(), fee.recordedAt(),
                fee.voidedAt());
    }
}
