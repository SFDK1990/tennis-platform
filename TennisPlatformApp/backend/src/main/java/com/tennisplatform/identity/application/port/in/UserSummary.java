package com.tennisplatform.identity.application.port.in;

import com.tennisplatform.identity.domain.User;

import java.time.Instant;
import java.util.UUID;

/**
 * The safe projection of an account: what may cross the wire. Notably absent is the password
 * hash, which must never leave the persistence layer.
 *
 * <p>The role and the status travel as strings rather than as {@code Role} and
 * {@code UserStatus}. They are enums in the domain and stay enums there; a caller outside this
 * module that received them would depend on identity's domain, which the module boundary rules
 * reject - and would reject rightly, because renaming a constant would then be a change to
 * every module at once. Same reasoning as {@code TeacherProfileView} turning a {@code ZoneId}
 * into its IANA id: a view carries wire values, not domain types.
 *
 * <p>{@code emailVerifiedAt} is here because {@code GET /me} answers with it, and reading it
 * from the account is the only way to be exact: the token's claim can lag by its whole
 * lifetime.
 */
public record UserSummary(UUID id, String email, String role, String status,
                          Instant emailVerifiedAt) {

    public static UserSummary of(User user) {
        return new UserSummary(user.id(), user.email().value(), user.role().name(),
                user.status().name(), user.emailVerifiedAt());
    }
}
