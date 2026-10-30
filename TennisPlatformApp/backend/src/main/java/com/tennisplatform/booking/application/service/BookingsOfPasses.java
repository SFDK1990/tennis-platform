package com.tennisplatform.booking.application.service;

import com.tennisplatform.booking.application.port.out.BookingRepository;
import com.tennisplatform.pass.application.port.spi.PassBookings;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * {@code pass}'s question about how many lessons each pass has spent, answered by the module
 * that has the bookings. It joins the caller's transaction: charging a pass counts after locking
 * it, and has to see every booking that got in before the lock.
 */
public class BookingsOfPasses implements PassBookings {

    private final BookingRepository bookings;

    public BookingsOfPasses(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Map<UUID, Integer> countConfirmed(Collection<UUID> passIds) {
        if (passIds.isEmpty()) {
            return Map.of();
        }
        return bookings.countConfirmedByPass(passIds);
    }
}
