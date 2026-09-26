package com.tennisplatform.booking.domain;

/** Attendance was marked before the lesson started. */
public class AttendanceNotYetOpenException extends RuntimeException {

    public AttendanceNotYetOpenException(String message) {
        super(message);
    }
}
