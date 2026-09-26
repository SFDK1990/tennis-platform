package com.tennisplatform.lesson.adapters.in.web;

import com.tennisplatform.error.Problems;
import com.tennisplatform.lesson.domain.InvalidLessonException;
import com.tennisplatform.lesson.domain.LessonAlreadyCancelledException;
import com.tennisplatform.lesson.domain.LessonAlreadyFinishedException;
import com.tennisplatform.lesson.domain.LessonInThePastException;
import com.tennisplatform.lesson.domain.LessonNotFoundException;
import com.tennisplatform.lesson.domain.LessonOutsideAvailabilityException;
import com.tennisplatform.lesson.domain.LessonOverlapException;
import com.tennisplatform.lesson.domain.LessonRangeTooWideException;
import com.tennisplatform.lesson.domain.TeacherRoleRequiredException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps lesson failures to Problem Details, following 11-contrato-api.md.
 *
 * <p>The split between 409 and 422 is the one that document sets: a 409 means the client was
 * working from a state that has moved on and re-reading may change the answer, a 422 means a
 * business rule that re-reading will not change. An overlap is a 409 because the other lesson
 * may be cancelled a second later; falling outside the configured hours is a 422 because the
 * only ways out are to move the lesson or to ask for it on purpose.
 *
 * <p>Selects by exception type rather than by controller, like the other module advices, so the
 * mapping still holds when another entry point raises the same failure - which it will, when
 * Fase 9 serves attendance from {@code booking} under a {@code /teacher/lessons} path.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class LessonExceptionHandler {

    @ExceptionHandler(LessonOverlapException.class)
    ProblemDetail handleOverlap(LessonOverlapException e) {
        return Problems.of(HttpStatus.CONFLICT, "Overlapping lesson", e.getMessage(), "LESSON_OVERLAP");
    }

    @ExceptionHandler(LessonOutsideAvailabilityException.class)
    ProblemDetail handleOutsideAvailability(LessonOutsideAvailabilityException e) {
        return Problems.of(HttpStatus.UNPROCESSABLE_ENTITY, "Outside the configured availability",
                e.getMessage(), "LESSON_OUTSIDE_AVAILABILITY");
    }

    @ExceptionHandler(LessonAlreadyCancelledException.class)
    ProblemDetail handleAlreadyCancelled(LessonAlreadyCancelledException e) {
        return Problems.of(HttpStatus.CONFLICT, "Lesson already cancelled", e.getMessage(),
                "LESSON_ALREADY_CANCELLED");
    }

    @ExceptionHandler(LessonAlreadyFinishedException.class)
    ProblemDetail handleAlreadyFinished(LessonAlreadyFinishedException e) {
        return Problems.of(HttpStatus.UNPROCESSABLE_ENTITY, "Lesson already finished", e.getMessage(),
                "LESSON_ALREADY_FINISHED");
    }

    @ExceptionHandler(LessonInThePastException.class)
    ProblemDetail handleInThePast(LessonInThePastException e) {
        return Problems.of(HttpStatus.UNPROCESSABLE_ENTITY, "Lesson in the past", e.getMessage(),
                "LESSON_IN_THE_PAST");
    }

    @ExceptionHandler(InvalidLessonException.class)
    ProblemDetail handleInvalid(InvalidLessonException e) {
        return Problems.of(HttpStatus.BAD_REQUEST, "Invalid lesson", e.getMessage(), "LESSON_INVALID");
    }

    @ExceptionHandler(LessonRangeTooWideException.class)
    ProblemDetail handleRangeTooWide(LessonRangeTooWideException e) {
        return Problems.of(HttpStatus.BAD_REQUEST, "Range too wide", e.getMessage(),
                "LESSON_RANGE_TOO_WIDE");
    }

    @ExceptionHandler(LessonNotFoundException.class)
    ProblemDetail handleNotFound(LessonNotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "Lesson not found", e.getMessage(), "LESSON_NOT_FOUND");
    }

    @ExceptionHandler(TeacherRoleRequiredException.class)
    ProblemDetail handleNotTheTeacher(TeacherRoleRequiredException e) {
        return Problems.of(HttpStatus.FORBIDDEN, "Only the teacher can do this", e.getMessage(),
                "TEACHER_FORBIDDEN");
    }
}
