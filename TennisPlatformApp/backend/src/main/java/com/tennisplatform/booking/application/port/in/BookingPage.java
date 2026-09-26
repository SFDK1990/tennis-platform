package com.tennisplatform.booking.application.port.in;

import java.util.List;

/**
 * A page of bookings, in the shape 11-contrato-api.md gives every listing.
 *
 * <p>The second page record in the codebase, after {@code ManagedStudentPage}. That one's javadoc
 * suggested extracting a generic page into {@code shared} once a second listing arrived; it was
 * not done, because it would mean letting every module depend on {@code shared} - a change to the
 * whole graph - to save a four-field record. See 20-fase9-analisis-booking.md.
 */
public record BookingPage(List<BookingView> items, int page, int size, long totalItems) {

    /** Copies the list on the way in, for the reason {@code ManagedStudentPage} explains. */
    public BookingPage {
        items = List.copyOf(items);
    }
}
