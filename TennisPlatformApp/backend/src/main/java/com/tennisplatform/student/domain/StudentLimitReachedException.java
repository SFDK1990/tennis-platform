package com.tennisplatform.student.domain;

/**
 * The teacher already manages as many students as the platform configuration allows. Maps to
 * 422: it is a business rule and not a concurrency conflict, following the criterion in
 * 11-contrato-api.md that reserves 409 for state the client should re-read.
 */
public class StudentLimitReachedException extends RuntimeException {

    public StudentLimitReachedException(String message) {
        super(message);
    }
}
