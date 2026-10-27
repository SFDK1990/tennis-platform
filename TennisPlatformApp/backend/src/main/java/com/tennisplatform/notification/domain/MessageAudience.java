package com.tennisplatform.notification.domain;

import java.util.Locale;

/**
 * Who a message is for: every student, a level, the students of one lesson, or those of its weekly
 * series (37-fase24-analisis-mensajes.md, decisions 1 and 2).
 */
public enum MessageAudience {
    ALL,
    LEVEL,
    LESSON,
    SERIES;

    /** Parsed here rather than by Spring's converter, whose failure on an unknown value comes back as a 500. */
    public static MessageAudience of(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidMessageException("A message needs an audience");
        }
        String normalised = value.trim().toUpperCase(Locale.ROOT);
        for (MessageAudience audience : values()) {
            if (audience.name().equals(normalised)) {
                return audience;
            }
        }
        throw new InvalidMessageException("Unknown audience: " + value);
    }
}
