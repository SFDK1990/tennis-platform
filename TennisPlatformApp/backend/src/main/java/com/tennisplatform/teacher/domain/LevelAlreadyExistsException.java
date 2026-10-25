package com.tennisplatform.teacher.domain;

/** The teacher already has a level with that name. Maps to 409. */
public class LevelAlreadyExistsException extends RuntimeException {

    public LevelAlreadyExistsException(String message) {
        super(message);
    }
}
