package com.tennisplatform.membership.domain;

/**
 * The day of the month by which a fee is due, the same for every group (decision 3). Up to the
 * 28th, so every month has it.
 */
public record FeeSettings(int dueDay) {

    public static final FeeSettings DEFAULT = new FeeSettings(5);

    public FeeSettings {
        if (dueDay < 1 || dueDay > 28) {
            throw new InvalidMembershipException("The due day goes from 1 to 28");
        }
    }
}
