package com.tennisplatform.notification.domain;

/**
 * The level or the lesson a message is for does not exist, or is another teacher's. Maps to 404
 * with the code of the thing that is missing, the same one the level and lesson endpoints answer.
 */
public class AudienceNotFoundException extends RuntimeException {

    private final String code;

    private AudienceNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static AudienceNotFoundException level(Object id) {
        return new AudienceNotFoundException("LEVEL_NOT_FOUND", "No level with id " + id);
    }

    public static AudienceNotFoundException lesson(Object id) {
        return new AudienceNotFoundException("LESSON_NOT_FOUND", "No lesson with id " + id);
    }

    public String code() {
        return code;
    }
}
