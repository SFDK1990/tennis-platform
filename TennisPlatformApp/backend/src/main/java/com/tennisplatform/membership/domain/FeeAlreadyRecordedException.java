package com.tennisplatform.membership.domain;

/** That month of the membership already has a fee recorded. Maps to 409. */
public class FeeAlreadyRecordedException extends RuntimeException {

    public FeeAlreadyRecordedException(String message) {
        super(message);
    }
}
