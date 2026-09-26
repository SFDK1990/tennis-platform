package com.tennisplatform.lesson.domain;

/**
 * The state a lesson is in when someone looks at it.
 *
 * <p>Only two of these are ever stored. {@code OPEN} and {@code CANCELLED} are decisions a person
 * made; {@code COMPLETED} and {@code FULL} are facts that follow from something else, and storing
 * a fact that follows from something else means keeping two copies in step for ever.
 *
 * <p>{@code COMPLETED} follows from the clock, so it needs no scheduled job - which is just as
 * well, because the MVP has none. {@code FULL} follows from the number of confirmed bookings,
 * which only {@code booking} can count, so the count is handed in when the status is worked
 * out. See {@code Lesson.statusAt}.
 */
public enum LessonStatus {
    OPEN,
    FULL,
    CANCELLED,
    COMPLETED
}
