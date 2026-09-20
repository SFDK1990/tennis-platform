package com.tennisplatform.availability.adapters.in.web;

import com.tennisplatform.availability.domain.AvailabilityOverrideNotFoundException;
import com.tennisplatform.availability.domain.AvailabilityRangeTooWideException;
import com.tennisplatform.availability.domain.InvalidAvailabilityException;
import com.tennisplatform.availability.domain.OverlappingAvailabilityRulesException;
import com.tennisplatform.availability.domain.TeacherRoleRequiredException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps availability failures to Problem Details, following 11-contrato-api.md.
 *
 * <p>All of these are 400 or 404 and none is a 422. A 422 is reserved for a business rule broken
 * by data that is otherwise valid; here the input is either malformed, self-contradictory or
 * points at something that is not there.
 *
 * <p>Selects by exception type rather than by controller, like the other module advices, so the
 * mapping still holds when another entry point raises the same failure.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class AvailabilityExceptionHandler {

    @ExceptionHandler(OverlappingAvailabilityRulesException.class)
    ProblemDetail handleOverlap(OverlappingAvailabilityRulesException e) {
        return problem(HttpStatus.BAD_REQUEST, "Overlapping availability rules", e.getMessage(),
                "AVAILABILITY_RULES_OVERLAP");
    }

    @ExceptionHandler(InvalidAvailabilityException.class)
    ProblemDetail handleInvalid(InvalidAvailabilityException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid availability", e.getMessage(),
                "AVAILABILITY_INVALID");
    }

    @ExceptionHandler(AvailabilityRangeTooWideException.class)
    ProblemDetail handleRangeTooWide(AvailabilityRangeTooWideException e) {
        return problem(HttpStatus.BAD_REQUEST, "Range too wide", e.getMessage(),
                "AVAILABILITY_RANGE_TOO_WIDE");
    }

    @ExceptionHandler(AvailabilityOverrideNotFoundException.class)
    ProblemDetail handleNotFound(AvailabilityOverrideNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Availability exception not found",
                "There is no availability exception with that id.",
                "AVAILABILITY_EXCEPTION_NOT_FOUND");
    }

    /**
     * The same code the other modules use for this, because it is the same answer to the client:
     * you are not the teacher. Only the class raising it belongs to this module.
     */
    @ExceptionHandler(TeacherRoleRequiredException.class)
    ProblemDetail handleNotTheTeacher(TeacherRoleRequiredException e) {
        return problem(HttpStatus.FORBIDDEN, "Not allowed",
                "Only the teacher can change the availability.", "TEACHER_FORBIDDEN");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
