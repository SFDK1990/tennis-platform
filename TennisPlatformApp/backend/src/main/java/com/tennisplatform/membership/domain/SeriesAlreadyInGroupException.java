package com.tennisplatform.membership.domain;

/** The series already belongs to a group. Maps to 409. */
public class SeriesAlreadyInGroupException extends RuntimeException {

    public SeriesAlreadyInGroupException(String message) {
        super(message);
    }
}
