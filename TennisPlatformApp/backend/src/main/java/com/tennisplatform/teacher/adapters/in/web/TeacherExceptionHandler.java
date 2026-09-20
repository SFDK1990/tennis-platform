package com.tennisplatform.teacher.adapters.in.web;

import com.tennisplatform.teacher.domain.InvalidTeacherProfileException;
import com.tennisplatform.teacher.domain.NotTheTeacherException;
import com.tennisplatform.teacher.domain.TeacherProfileNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps teacher failures to Problem Details, following the mapping in 11-contrato-api.md.
 *
 * <p>Unlike the identity handler, these messages may be specific: none of them tells the caller
 * anything they could not already find out, and a vague answer here would only make a
 * misconfigured deployment harder to diagnose.
 *
 * <p>It advises every controller rather than only this module's, which is a correction to how
 * Fase 6 first wrote it. {@code PATCH /me} is composed at the web edge and can raise these same
 * exceptions; tying the mapping to one controller meant either a 500 from the orchestrator or
 * the same map copied into it. The handlers select by exception type, so a module-owned advice
 * still only ever answers for its own failures.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class TeacherExceptionHandler {

    @ExceptionHandler(TeacherProfileNotFoundException.class)
    ProblemDetail handleNotFound(TeacherProfileNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Teacher profile not found",
                "There is no teacher profile yet.", "TEACHER_PROFILE_NOT_FOUND");
    }

    @ExceptionHandler(NotTheTeacherException.class)
    ProblemDetail handleNotTheTeacher(NotTheTeacherException e) {
        return problem(HttpStatus.FORBIDDEN, "Not allowed",
                "Only the teacher can change this profile.", "TEACHER_FORBIDDEN");
    }

    @ExceptionHandler(InvalidTeacherProfileException.class)
    ProblemDetail handleInvalidProfile(InvalidTeacherProfileException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid teacher profile", e.getMessage(),
                "TEACHER_PROFILE_INVALID");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setProperty("code", code);
        return problem;
    }
}
