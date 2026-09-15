package com.tennisplatform.identity.application.port.in;

import com.tennisplatform.identity.domain.Role;
import com.tennisplatform.identity.domain.User;
import com.tennisplatform.identity.domain.UserStatus;

import java.util.UUID;

/**
 * The safe projection of an account: what may cross the wire. Notably absent is the password
 * hash, which must never leave the persistence layer.
 */
public record UserSummary(UUID id, String email, Role role, UserStatus status) {

    public static UserSummary of(User user) {
        return new UserSummary(user.id(), user.email().value(), user.role(), user.status());
    }
}
