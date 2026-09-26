package com.tennisplatform.identity.application.port.in;

import com.tennisplatform.identity.domain.Role;

import java.util.UUID;

/**
 * Identity as carried by a verified access token. This - never a path variable or a body
 * field - is the source of truth for who is calling.
 *
 * <p>{@code emailVerified} is a snapshot taken when the token was issued, so it can lag
 * reality by at most the access token lifetime. Acceptable for gating bookings; anything
 * that must be exact has to re-read the user.
 *
 * <p>It lives among the inbound ports because it is part of what {@code identity} offers to the
 * rest of the application: every other module needs to know who is calling, and no module may
 * reach into another one's adapters to find out. It used to sit in
 * {@code adapters/out/security}, where the first controller outside {@code identity} could only
 * have used it by importing another module's adapter.
 */
public record AuthenticatedUser(UUID id, Role role, boolean emailVerified) {

    /**
     * Asking here instead of comparing against {@link Role} outside is what keeps the role check
     * inside this module. A caller writing {@code caller.role() == Role.TEACHER} would import
     * identity's domain, which the module boundary rules reject - verified by deliberately
     * introducing that comparison and watching the rule fail.
     */
    public boolean isTeacher() {
        return role == Role.TEACHER;
    }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
