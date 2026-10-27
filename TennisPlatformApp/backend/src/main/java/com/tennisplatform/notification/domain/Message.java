package com.tennisplatform.notification.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Something the teacher wrote to some of their students (37-fase24-analisis-mensajes.md). It is
 * never edited: the email has already gone out, and a different version on the board would
 * contradict it.
 *
 * @param audienceLabel who it went to, written when it was sent: the level or the lesson can be
 *                      renamed or disappear afterwards
 * @param recipientCount how many students it reached when it was sent, which does not shrink when
 *                       one of them later deletes their account
 */
public record Message(UUID id, UUID teacherUserId, MessageAudience audience, String audienceLabel, String subject,
                      String body, Instant sentAt, int recipientCount) {

    public static final int MAX_SUBJECT = 120;
    public static final int MAX_BODY = 4000;

    public Message {
        subject = required(subject, "subject", MAX_SUBJECT);
        body = required(body, "body", MAX_BODY);
    }

    private static String required(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw new InvalidMessageException("A message needs a " + field);
        }
        String trimmed = value.strip();
        if (trimmed.length() > max) {
            throw new InvalidMessageException("A message's " + field + " has at most " + max + " characters");
        }
        return trimmed;
    }

    public Message sentTo(int recipients) {
        return new Message(id, teacherUserId, audience, audienceLabel, subject, body, sentAt, recipients);
    }
}
