package com.tennisplatform.teacher.domain;

/**
 * No teacher profile exists yet. In the MVP this means the bootstrap has not run - there is
 * exactly one teacher and no endpoint creates them - so it is an operational problem, not a
 * client mistake. Maps to 404 because the resource genuinely is not there.
 */
public class TeacherProfileNotFoundException extends RuntimeException {

    public TeacherProfileNotFoundException(String message) {
        super(message);
    }
}
