package com.tennisplatform.availability.domain;

/**
 * Two rules of the same weekday whose active periods and hours both overlap.
 *
 * <p>Separate from {@link InvalidAvailabilityException} because the caller can act on it: the
 * offending pair is named, so a client can point at the two rows instead of the whole form.
 */
public class OverlappingAvailabilityRulesException extends RuntimeException {

    public OverlappingAvailabilityRulesException(String message) {
        super(message);
    }
}
