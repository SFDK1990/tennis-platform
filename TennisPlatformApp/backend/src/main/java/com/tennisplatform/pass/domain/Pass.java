package com.tennisplatform.pass.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

/**
 * A pass the teacher has sold a student (decision 3). It copies the name, lessons, price and
 * validity of its type when it is recorded: the teacher may change the type tomorrow, and what
 * the student paid for does not change with it.
 *
 * <p>How many lessons are left is not kept here. It is {@code lessons} minus the confirmed
 * bookings paid with the pass, which only {@code booking} knows: a cancellation, whoever makes
 * it, gives the lesson back by itself, and the lesson goes back to the pass it came from even
 * if that pass has expired since (decision 5).
 *
 * <p>{@code validUntil} is the last day it can be used, in the teacher's zone, or null.
 */
public record Pass(UUID id, UUID teacherUserId, UUID studentUserId, UUID passTypeId, String name,
                   PassLessonType lessonType, int lessons, int priceCents, LocalDate paidOn,
                   PaymentMethod paymentMethod, LocalDate validUntil, Instant createdAt, Instant voidedAt) {

    /**
     * Which pass a lesson comes out of when several could pay for it: the one that expires first,
     * so none is lost with lessons still on it, and one that never expires last (decision 4).
     * Between two that expire on the same day, the older one.
     */
    public static final Comparator<Pass> SPENDING_ORDER = Comparator
            .comparing(Pass::validUntil, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Pass::createdAt);

    /** The pass a student has just paid for. It lasts from the day it was paid. */
    public static Pass sell(PassType type, UUID studentUserId, LocalDate paidOn, PaymentMethod method, Instant now) {
        if (paidOn == null) {
            throw new InvalidPassException("Say when the pass was paid");
        }
        if (method == null) {
            throw new InvalidPassException("Say how the pass was paid: CASH, BIZUM or TRANSFER");
        }
        if (type.retired()) {
            throw new PassTypeRetiredException("This pass type is no longer sold");
        }
        LocalDate validUntil = type.validityMonths() == null ? null : paidOn.plusMonths(type.validityMonths());
        return new Pass(null, type.teacherUserId(), studentUserId, type.id(), type.name(), type.lessonType(),
                type.lessons(), type.priceCents(), paidOn, method, validUntil, now, null);
    }

    /**
     * Cancels a pass recorded by mistake. Only while no lesson has been booked with it: after
     * that, the lessons it paid for would be left paid by nothing.
     */
    public Pass voided(int confirmedBookings, Instant now) {
        if (voidedAt != null) {
            return this;
        }
        if (confirmedBookings > 0) {
            throw new PassAlreadyUsedException("A lesson was already booked with this pass");
        }
        return new Pass(id, teacherUserId, studentUserId, passTypeId, name, lessonType, lessons, priceCents,
                paidOn, paymentMethod, validUntil, createdAt, now);
    }

    public boolean isVoided() {
        return voidedAt != null;
    }

    /**
     * Whether it can pay for a lesson on {@code day}, the lesson's date in the teacher's zone. A
     * pass "until 15 January" pays for lessons up to that day; an expired one pays for nothing,
     * even with lessons left (decision 4).
     */
    public boolean coversDay(LocalDate day) {
        return !isVoided() && (validUntil == null || !day.isAfter(validUntil));
    }

    public int remaining(int confirmedBookings) {
        return Math.max(0, lessons - confirmedBookings);
    }

    /** How the pass stands on {@code today}, for the screens: the rules ask {@link #coversDay}. */
    public PassStatus statusOn(LocalDate today, int confirmedBookings) {
        if (isVoided()) {
            return PassStatus.VOIDED;
        }
        if (remaining(confirmedBookings) == 0) {
            return PassStatus.USED_UP;
        }
        if (validUntil != null && today.isAfter(validUntil)) {
            return PassStatus.EXPIRED;
        }
        return PassStatus.ACTIVE;
    }
}
