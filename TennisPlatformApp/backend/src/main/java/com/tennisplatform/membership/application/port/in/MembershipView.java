package com.tennisplatform.membership.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A student's place in a group, with their months up to now and any recorded ahead. */
public record MembershipView(UUID id, GroupView group, Instant joinedAt, Instant leftAt, List<FeeLineView> fees) {

    public MembershipView {
        fees = List.copyOf(fees);
    }
}
