package com.tennisplatform.notification.application.port.in;

import com.tennisplatform.notification.domain.Message;

import java.time.Instant;
import java.util.UUID;

/** A message as the teacher sees it among the sent ones. The audience travels as a string, like every view. */
public record SentMessageView(UUID id, String audience, String audienceLabel, String subject, String body,
                              Instant sentAt, int recipientCount) {

    public static SentMessageView from(Message message) {
        return new SentMessageView(message.id(), message.audience().name(), message.audienceLabel(),
                message.subject(), message.body(), message.sentAt(), message.recipientCount());
    }
}
