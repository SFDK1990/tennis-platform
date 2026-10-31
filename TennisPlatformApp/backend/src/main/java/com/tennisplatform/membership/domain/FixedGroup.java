package com.tennisplatform.membership.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A fixed group of the teacher (decision 1): a name and a monthly fee. Its lessons are those of
 * the weekly series attached to it, so a new term is a new series on the same group, and its
 * members keep their place.
 *
 * <p>The fee is what a month costs by default. Each payment records the amount actually paid, so
 * changing the fee does not rewrite what was already recorded.
 */
public record FixedGroup(UUID id, UUID teacherUserId, String name, int monthlyFeeCents, Instant createdAt) {

    public static final int MAX_NAME_LENGTH = 60;
    public static final int MAX_FEE_CENTS = 1_000_000;

    public FixedGroup {
        if (name == null || name.isBlank()) {
            throw new InvalidMembershipException("A group needs a name");
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidMembershipException("A group name has at most " + MAX_NAME_LENGTH + " characters");
        }
        if (monthlyFeeCents < 0 || monthlyFeeCents > MAX_FEE_CENTS) {
            throw new InvalidMembershipException("The monthly fee goes from 0 to " + MAX_FEE_CENTS + " cents");
        }
    }

    public static FixedGroup create(UUID teacherUserId, String name, int monthlyFeeCents, Instant now) {
        return new FixedGroup(null, teacherUserId, name, monthlyFeeCents, now);
    }

    /** The same group with what was sent changed; null keeps the value. */
    public FixedGroup changed(String newName, Integer newFeeCents) {
        return new FixedGroup(id, teacherUserId, newName == null ? name : newName,
                newFeeCents == null ? monthlyFeeCents : newFeeCents, createdAt);
    }
}
