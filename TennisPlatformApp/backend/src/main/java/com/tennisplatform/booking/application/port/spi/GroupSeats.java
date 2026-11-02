package com.tennisplatform.booking.application.port.spi;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Whether a seat is paid by the monthly fee of a fixed group (43-fase28-analisis-mensualidades.md).
 *
 * <p>Asked by every path that gives a seat: a member who cancelled a lesson of their group and
 * books it again, or who gets it back from the waiting list, holds it as the group's and spends
 * no pass.
 */
public interface GroupSeats {

    /**
     * The membership of the student in the group that holds {@code seriesId}, if they are signed
     * up to it on the day of the lesson.
     */
    Optional<UUID> membershipPaying(UUID studentUserId, UUID seriesId, Instant lessonStartsAt);
}
