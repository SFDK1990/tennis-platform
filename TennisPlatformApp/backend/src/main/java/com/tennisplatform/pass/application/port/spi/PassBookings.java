package com.tennisplatform.pass.application.port.spi;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** What this module needs to know about the bookings paid with its passes, which only {@code booking} can. */
public interface PassBookings {

    /**
     * The confirmed bookings paid with each pass, inside the caller's transaction. A pass with
     * none is absent from the map.
     */
    Map<UUID, Integer> countConfirmed(Collection<UUID> passIds);
}
