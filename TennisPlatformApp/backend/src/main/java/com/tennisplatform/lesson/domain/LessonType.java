package com.tennisplatform.lesson.domain;

import java.util.Locale;

/**
 * Whether the lesson is for one student or for several.
 *
 * <p>The type is not a label on top of the capacity: it decides it. An individual lesson has
 * capacity one and cannot have any other, which is enforced here and again by a CHECK in the
 * schema.
 */
public enum LessonType {
    INDIVIDUAL,
    GROUP;

    /** Parses the wire value, rejecting anything else rather than defaulting to one of the two. */
    public static LessonType parse(String value) {
        if (value == null) {
            throw new InvalidLessonException("A lesson must be either INDIVIDUAL or GROUP");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidLessonException(
                    "Unknown lesson type: " + value + ". Expected INDIVIDUAL or GROUP");
        }
    }
}
