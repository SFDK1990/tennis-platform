package com.tennisplatform.student.domain;

/** An objective cannot be achieved on a day that has not come yet. Maps to 422. */
public class AchievedOnInFutureException extends RuntimeException {

    public AchievedOnInFutureException(String message) {
        super(message);
    }
}
