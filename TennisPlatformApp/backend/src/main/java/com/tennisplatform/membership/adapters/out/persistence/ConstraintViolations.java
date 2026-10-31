package com.tennisplatform.membership.adapters.out.persistence;

import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

/** PostgreSQL SQLStates, which survive a change of wording or locale. */
final class ConstraintViolations {

    static final String UNIQUE_VIOLATION = "23505";

    private ConstraintViolations() {
    }

    static boolean is(DataIntegrityViolationException e, String state) {
        return e.getMostSpecificCause() instanceof SQLException cause && state.equals(cause.getSQLState());
    }
}
