package com.tennisplatform.membership.adapters.in.web;

import com.tennisplatform.error.Problems;
import com.tennisplatform.membership.domain.AlreadyInGroupException;
import com.tennisplatform.membership.domain.EmailNotVerifiedException;
import com.tennisplatform.membership.domain.FeeAlreadyRecordedException;
import com.tennisplatform.membership.domain.FeeNotFoundException;
import com.tennisplatform.membership.domain.GroupAlreadyExistsException;
import com.tennisplatform.membership.domain.GroupFullException;
import com.tennisplatform.membership.domain.GroupInUseException;
import com.tennisplatform.membership.domain.GroupNotFoundException;
import com.tennisplatform.membership.domain.InvalidMembershipException;
import com.tennisplatform.membership.domain.NotInGroupException;
import com.tennisplatform.membership.domain.SeriesAlreadyInGroupException;
import com.tennisplatform.membership.domain.SeriesNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps membership failures to Problem Details, with the 409/422 split of 11-contrato-api.md. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class MembershipExceptionHandler {

    @ExceptionHandler(GroupNotFoundException.class)
    ProblemDetail handleGroupNotFound(GroupNotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "Group not found", e.getMessage(), "GROUP_NOT_FOUND");
    }

    @ExceptionHandler(GroupAlreadyExistsException.class)
    ProblemDetail handleGroupExists(GroupAlreadyExistsException e) {
        return Problems.of(HttpStatus.CONFLICT, "Group already exists", e.getMessage(), "GROUP_ALREADY_EXISTS");
    }

    @ExceptionHandler(GroupInUseException.class)
    ProblemDetail handleGroupInUse(GroupInUseException e) {
        return Problems.of(HttpStatus.CONFLICT, "Group in use", e.getMessage(), "GROUP_IN_USE");
    }

    /** The starts of the full lessons go along, so the screen can say which dates to fix. */
    @ExceptionHandler(GroupFullException.class)
    ProblemDetail handleGroupFull(GroupFullException e) {
        ProblemDetail problem = Problems.of(HttpStatus.CONFLICT, "Group full", e.getMessage(), "GROUP_FULL");
        problem.setProperty("fullLessons", e.fullLessons().stream().map(Object::toString).toList());
        return problem;
    }

    @ExceptionHandler(AlreadyInGroupException.class)
    ProblemDetail handleAlreadyIn(AlreadyInGroupException e) {
        return Problems.of(HttpStatus.CONFLICT, "Already in the group", e.getMessage(), "ALREADY_IN_GROUP");
    }

    @ExceptionHandler(NotInGroupException.class)
    ProblemDetail handleNotIn(NotInGroupException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "Not in the group", e.getMessage(), "NOT_IN_GROUP");
    }

    @ExceptionHandler(SeriesNotFoundException.class)
    ProblemDetail handleSeriesNotFound(SeriesNotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "Series not found", e.getMessage(), "SERIES_NOT_FOUND");
    }

    @ExceptionHandler(SeriesAlreadyInGroupException.class)
    ProblemDetail handleSeriesInGroup(SeriesAlreadyInGroupException e) {
        return Problems.of(HttpStatus.CONFLICT, "Series already in a group", e.getMessage(),
                "SERIES_ALREADY_IN_GROUP");
    }

    @ExceptionHandler(FeeAlreadyRecordedException.class)
    ProblemDetail handleFeeRecorded(FeeAlreadyRecordedException e) {
        return Problems.of(HttpStatus.CONFLICT, "Fee already recorded", e.getMessage(), "FEE_ALREADY_PAID");
    }

    @ExceptionHandler(FeeNotFoundException.class)
    ProblemDetail handleFeeNotFound(FeeNotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "Fee not found", e.getMessage(), "FEE_NOT_FOUND");
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    ProblemDetail handleEmailNotVerified(EmailNotVerifiedException e) {
        return Problems.of(HttpStatus.FORBIDDEN, "Email not verified", e.getMessage(), "EMAIL_NOT_VERIFIED");
    }

    @ExceptionHandler(InvalidMembershipException.class)
    ProblemDetail handleInvalid(InvalidMembershipException e) {
        return Problems.of(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage(), "VALIDATION_ERROR");
    }
}
