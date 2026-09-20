package com.tennisplatform.student.domain;

public enum ManagedStatus {

    /** The teacher currently manages this student, who may therefore book lessons. */
    MANAGED,

    /**
     * The teacher stopped managing this student. The row survives deactivation so the history
     * is not lost and so taking the student back cannot become a second relationship.
     */
    INACTIVE
}
