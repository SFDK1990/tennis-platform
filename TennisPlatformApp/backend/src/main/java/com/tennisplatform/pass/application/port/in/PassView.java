package com.tennisplatform.pass.application.port.in;

import com.tennisplatform.pass.domain.Pass;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A pass with what is left of it. {@code used} counts the confirmed bookings paid with it, the
 * ones the student did not come to included: that lesson was spent when it was booked
 * (decision 5).
 */
public record PassView(UUID id, UUID studentUserId, UUID passTypeId, String name, String lessonType, int lessons,
                       int used, int remaining, int priceCents, LocalDate paidOn, String paymentMethod,
                       LocalDate validUntil, String status, Instant createdAt, Instant voidedAt) {

    public static PassView from(Pass pass, int used, LocalDate today) {
        return new PassView(pass.id(), pass.studentUserId(), pass.passTypeId(), pass.name(), pass.lessonType().name(),
                pass.lessons(), used, pass.remaining(used), pass.priceCents(), pass.paidOn(),
                pass.paymentMethod().name(), pass.validUntil(), pass.statusOn(today, used).name(), pass.createdAt(),
                pass.voidedAt());
    }
}
