package com.tennisplatform.notification.domain;

/**
 * A message to a lesson's group, and the lesson was created on its own. Maps to 422, like
 * cancelling its following ones.
 */
public class LessonNotInSeriesException extends RuntimeException {

    public LessonNotInSeriesException(String message) {
        super(message);
    }
}
