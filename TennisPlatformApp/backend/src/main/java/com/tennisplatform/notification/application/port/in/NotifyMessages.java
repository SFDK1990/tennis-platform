package com.tennisplatform.notification.application.port.in;

import java.util.UUID;

/**
 * Emails a message to its recipients (Fase 24). Called after the message's transaction has
 * committed; like {@link NotifyBookings}, it never throws.
 */
public interface NotifyMessages {

    /** One email per recipient whose address is verified; the rest read it on their board. */
    void sent(UUID messageId);
}
