package com.tennisplatform.availability.domain;

/** A rule or exception that cannot exist: an empty interval, a day out of range, a half-specified exception. */
public class InvalidAvailabilityException extends RuntimeException {

    public InvalidAvailabilityException(String message) {
        super(message);
    }
}
