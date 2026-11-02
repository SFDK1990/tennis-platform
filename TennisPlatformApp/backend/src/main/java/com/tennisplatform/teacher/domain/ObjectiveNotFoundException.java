package com.tennisplatform.teacher.domain;

/** No live objective with that id among the teacher's. Maps to 404. */
public class ObjectiveNotFoundException extends RuntimeException {

    public ObjectiveNotFoundException(String message) {
        super(message);
    }
}
