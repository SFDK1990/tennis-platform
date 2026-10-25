package com.tennisplatform.teacher.domain;

/** A student or a lesson still has the level, so it cannot be deleted. Maps to 409. */
public class LevelInUseException extends RuntimeException {

    public LevelInUseException(String message) {
        super(message);
    }
}
