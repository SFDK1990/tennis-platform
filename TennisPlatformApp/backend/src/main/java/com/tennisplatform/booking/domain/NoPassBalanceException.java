package com.tennisplatform.booking.domain;

/**
 * The teacher requires a pass to book, and the student has no lesson left for this kind of lesson
 * on a pass valid that day (Fase 27). Maps to 422: re-reading will not change it, only a new pass.
 */
public class NoPassBalanceException extends RuntimeException {

    public NoPassBalanceException(String message) {
        super(message);
    }
}
