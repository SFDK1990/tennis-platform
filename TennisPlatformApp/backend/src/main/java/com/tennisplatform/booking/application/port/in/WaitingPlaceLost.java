package com.tennisplatform.booking.application.port.in;

import java.util.UUID;

/**
 * A seat came free for the first student on a waiting list, and they could not take it because
 * they had no lesson left on their passes (Fase 27, decision 5). They are off the list and the
 * seat went to the next one; they are told why. Heard after the commit, like {@link BookingConfirmed}.
 */
public record WaitingPlaceLost(UUID lessonId, UUID teacherUserId, UUID studentUserId) {
}
