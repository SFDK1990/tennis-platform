package com.tennisplatform.teacher.domain;

/** No level with that id among the teacher's. Maps to 404. */
public class LevelNotFoundException extends RuntimeException {

    public LevelNotFoundException(String message) {
        super(message);
    }
}
