package com.tennisplatform.booking.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * A booking has just been confirmed. Published inside the booking's transaction and heard after
 * it commits, so nobody reacts to a seat that a rollback took back.
 *
 * <p>Ids and times only: whoever reacts looks up the names and addresses it needs, which keeps
 * personal data out of anything that may end up queued or logged.
 */
public record BookingConfirmed(UUID bookingId, UUID lessonId, UUID teacherUserId, UUID studentUserId,
                               Instant startsAt, Instant endsAt) {
}
