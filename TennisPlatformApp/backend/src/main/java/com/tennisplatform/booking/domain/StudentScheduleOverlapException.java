package com.tennisplatform.booking.domain;

/** The student already has a confirmed booking at an overlapping time. */
public class StudentScheduleOverlapException extends RuntimeException {

    public StudentScheduleOverlapException(String message) {
        super(message);
    }
}
