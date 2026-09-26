package com.tennisplatform.lesson.application.port.in;

import java.time.Instant;

/**
 * What the caller asks for when scheduling a lesson.
 *
 * <p>{@code type} arrives as the wire string and is parsed inside the module, so an unknown
 * value is rejected by the same rule that rejects every other bad input instead of by the JSON
 * binder with a different shape of answer.
 *
 * <p>{@code overrideAvailability} is the explicit action the product asks for: a lesson outside
 * the configured hours is a decision, not an accident, and it has to be asked for by name.
 */
public record NewLesson(String type, Instant startsAt, Instant endsAt, int capacity, String notes,
                        boolean overrideAvailability) {
}
