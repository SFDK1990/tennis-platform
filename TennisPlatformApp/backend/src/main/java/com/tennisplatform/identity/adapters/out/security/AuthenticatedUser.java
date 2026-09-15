package com.tennisplatform.identity.adapters.out.security;

import com.tennisplatform.identity.domain.Role;

import java.util.UUID;

/**
 * Identity as carried by a verified access token. This - never a path variable or a body
 * field - is the source of truth for who is calling.
 *
 * <p>{@code emailVerified} is a snapshot taken when the token was issued, so it can lag
 * reality by at most the access token lifetime. Acceptable for gating bookings; anything
 * that must be exact has to re-read the user.
 */
public record AuthenticatedUser(UUID id, Role role, boolean emailVerified) {
}
