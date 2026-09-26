package com.tennisplatform.identity.application.port.in;

import com.tennisplatform.identity.domain.User;

import java.time.Instant;
import java.util.UUID;

/** An account as the administration console lists it. Wire values, like {@link UserSummary}. */
public record AccountView(UUID id, String email, String role, String status, Instant createdAt) {

    public static AccountView of(User user) {
        return new AccountView(user.id(), user.email().value(), user.role().name(), user.status().name(),
                user.createdAt());
    }

    public boolean isStudent() {
        return "STUDENT".equals(role);
    }
}
