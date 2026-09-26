package com.tennisplatform.booking.application.port.in;

import java.util.UUID;

/** A student takes a seat in a lesson. Confirmed at once: the MVP has no approval step. */
public interface BookLesson {

    /**
     * @param studentUserId always taken from the authenticated token, never from the request
     * @param emailVerified what the token says; see {@code EmailNotVerifiedException}
     */
    BookingView book(UUID studentUserId, boolean emailVerified, UUID lessonId);
}
