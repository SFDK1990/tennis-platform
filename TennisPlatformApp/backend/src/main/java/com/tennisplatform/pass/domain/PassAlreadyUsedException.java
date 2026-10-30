package com.tennisplatform.pass.domain;

/** A lesson was already booked with the pass, so it can no longer be voided. Maps to 409. */
public class PassAlreadyUsedException extends RuntimeException {

    public PassAlreadyUsedException(String message) {
        super(message);
    }
}
