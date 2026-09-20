package com.tennisplatform.availability.domain;

/** No exception with that id belongs to this teacher. Deliberately the same answer as "it does not exist". */
public class AvailabilityOverrideNotFoundException extends RuntimeException {

    public AvailabilityOverrideNotFoundException(String message) {
        super(message);
    }
}
