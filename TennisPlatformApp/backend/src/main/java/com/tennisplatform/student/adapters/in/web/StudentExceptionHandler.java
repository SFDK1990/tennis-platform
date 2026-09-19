package com.tennisplatform.student.adapters.in.web;

import com.tennisplatform.student.domain.InvalidStudentProfileException;
import com.tennisplatform.student.domain.StudentAlreadyInactiveException;
import com.tennisplatform.student.domain.StudentAlreadyManagedException;
import com.tennisplatform.student.domain.StudentLimitReachedException;
import com.tennisplatform.student.domain.StudentNotManagedException;
import com.tennisplatform.student.domain.StudentProfileIncompleteException;
import com.tennisplatform.student.domain.StudentProfileNotFoundException;
import com.tennisplatform.student.domain.TeacherRoleRequiredException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps student failures to Problem Details, following the mapping in 11-contrato-api.md: 409
 * for state the client should re-read, 422 for a business rule that no re-read will change,
 * 403 for a missing relationship.
 *
 * <p>The messages never carry personal data. In particular a refusal says that the student is
 * not managed, never who they are - an error body is as readable as a listing.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class StudentExceptionHandler {

    @ExceptionHandler(StudentProfileNotFoundException.class)
    ProblemDetail handleNotFound(StudentProfileNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Student not found", e.getMessage(),
                "STUDENT_NOT_FOUND");
    }

    /**
     * The code the API contract already defines for "no relationship with this student". It is
     * what a teacher gets when they ask about somebody else's student, and what keeps knowing
     * a UUID from being the same thing as being allowed to look.
     */
    @ExceptionHandler(StudentNotManagedException.class)
    ProblemDetail handleNotManaged(StudentNotManagedException e) {
        return problem(HttpStatus.FORBIDDEN, "Not allowed",
                "This student is not managed by you.", "STUDENT_NOT_MANAGED");
    }

    @ExceptionHandler(TeacherRoleRequiredException.class)
    ProblemDetail handleNotTheTeacher(TeacherRoleRequiredException e) {
        return problem(HttpStatus.FORBIDDEN, "Not allowed",
                "Only the teacher can manage students.", "TEACHER_FORBIDDEN");
    }

    @ExceptionHandler(StudentAlreadyManagedException.class)
    ProblemDetail handleAlreadyManaged(StudentAlreadyManagedException e) {
        return problem(HttpStatus.CONFLICT, "Already managed",
                "This student is already managed.", "STUDENT_ALREADY_MANAGED");
    }

    @ExceptionHandler(StudentAlreadyInactiveException.class)
    ProblemDetail handleAlreadyInactive(StudentAlreadyInactiveException e) {
        return problem(HttpStatus.CONFLICT, "Already deactivated",
                "This student is already deactivated.", "STUDENT_ALREADY_INACTIVE");
    }

    @ExceptionHandler(StudentLimitReachedException.class)
    ProblemDetail handleLimitReached(StudentLimitReachedException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Student limit reached", e.getMessage(),
                "STUDENT_LIMIT_REACHED");
    }

    @ExceptionHandler(StudentProfileIncompleteException.class)
    ProblemDetail handleIncompleteProfile(StudentProfileIncompleteException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Student profile incomplete",
                e.getMessage(), "STUDENT_PROFILE_INCOMPLETE");
    }

    @ExceptionHandler(InvalidStudentProfileException.class)
    ProblemDetail handleInvalidProfile(InvalidStudentProfileException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid student profile", e.getMessage(),
                "STUDENT_PROFILE_INVALID");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
