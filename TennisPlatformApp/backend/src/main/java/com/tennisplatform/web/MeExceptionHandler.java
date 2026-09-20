package com.tennisplatform.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The one failure {@code /me} owns itself. Everything else it can raise belongs to a module,
 * and is mapped by that module's advice - which is why those advices are not tied to their own
 * controllers.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class MeExceptionHandler {

    @ExceptionHandler(FieldNotApplicableToRoleException.class)
    ProblemDetail handleFieldNotApplicable(FieldNotApplicableToRoleException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Field not applicable");
        problem.setDetail(e.getMessage());
        problem.setProperty("code", "FIELD_NOT_APPLICABLE_TO_ROLE");
        return problem;
    }
}
