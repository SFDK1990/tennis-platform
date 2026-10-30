package com.tennisplatform.pass.domain;

/** The teacher retired the pass type, so it is no longer sold. Maps to 422. */
public class PassTypeRetiredException extends RuntimeException {

    public PassTypeRetiredException(String message) {
        super(message);
    }
}
