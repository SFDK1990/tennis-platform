package com.tennisplatform.booking.domain;

/**
 * The caller's role is not one this operation serves: a teacher trying to book a seat, an admin
 * asking for "my bookings". Maps to {@code AUTH_FORBIDDEN}, the code the security layer already
 * uses for "authenticated, but not allowed to do this".
 */
public class RoleNotAllowedException extends RuntimeException {

    public RoleNotAllowedException(String message) {
        super(message);
    }
}
