package com.tennisplatform.lesson.domain;

/** The day to cancel has already gone by in the teacher's zone (Fase 20). */
public class DateInThePastException extends RuntimeException {

    public DateInThePastException(String message) {
        super(message);
    }
}
