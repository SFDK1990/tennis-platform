package com.tennisplatform.availability.domain;

/** The requested date range exceeds the cap that keeps a read from becoming unbounded. */
public class AvailabilityRangeTooWideException extends RuntimeException {

    public AvailabilityRangeTooWideException(String message) {
        super(message);
    }
}
