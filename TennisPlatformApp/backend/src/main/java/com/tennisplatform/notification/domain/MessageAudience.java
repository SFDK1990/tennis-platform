package com.tennisplatform.notification.domain;

import java.util.Locale;

/**
 * Who a message is for: every student, a level, the students of one lesson, or those of its weekly
 * series (37-fase24-analisis-mensajes.md, decisions 1 and 2). {@code PROGRESS} is the notice the
 * app writes on one student's board when the teacher marks an objective as achieved (Fase 29): the
 * teacher never writes one, and it is not among the sent messages.
 */
public enum MessageAudience {
    ALL,
    LEVEL,
    LESSON,
    SERIES,
    PROGRESS;

    /** Parsed here rather than by Spring's converter, whose failure on an unknown value comes back as a 500. */
    public static MessageAudience of(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidMessageException("A message needs an audience");
        }
        String normalised = value.trim().toUpperCase(Locale.ROOT);
        for (MessageAudience audience : values()) {
            if (audience != PROGRESS && audience.name().equals(normalised)) {
                return audience;
            }
        }
        throw new InvalidMessageException("Unknown audience: " + value);
    }
}
