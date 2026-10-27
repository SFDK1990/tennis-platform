package com.tennisplatform.notification.application.port.in;

import com.tennisplatform.notification.domain.Message;

import java.time.Instant;
import java.util.UUID;

/** A message on a student's board: what it says, and nothing about who else got it. */
public record ReceivedMessageView(UUID id, String subject, String body, Instant sentAt) {

    public static ReceivedMessageView from(Message message) {
        return new ReceivedMessageView(message.id(), message.subject(), message.body(), message.sentAt());
    }
}
