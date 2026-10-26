package com.tennisplatform.notification.adapters.in.web;

import com.tennisplatform.error.Problems;
import com.tennisplatform.notification.domain.AudienceNotFoundException;
import com.tennisplatform.notification.domain.InvalidMessageException;
import com.tennisplatform.notification.domain.LessonNotInSeriesException;
import com.tennisplatform.notification.domain.MessageNotFoundException;
import com.tennisplatform.notification.domain.NoRecipientsException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps the failures of the messages to Problem Details (11-contrato-api.md). The texts never quote the message. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class NotificationExceptionHandler {

    @ExceptionHandler(InvalidMessageException.class)
    ProblemDetail handleInvalid(InvalidMessageException e) {
        return Problems.of(HttpStatus.BAD_REQUEST, "Invalid message", e.getMessage(), "VALIDATION_ERROR");
    }

    @ExceptionHandler(NoRecipientsException.class)
    ProblemDetail handleNoRecipients(NoRecipientsException e) {
        return Problems.of(HttpStatus.UNPROCESSABLE_CONTENT, "No recipients", e.getMessage(), "NO_RECIPIENTS");
    }

    @ExceptionHandler(LessonNotInSeriesException.class)
    ProblemDetail handleNotInSeries(LessonNotInSeriesException e) {
        return Problems.of(HttpStatus.UNPROCESSABLE_CONTENT, "Lesson not in a series", e.getMessage(),
                "LESSON_NOT_IN_SERIES");
    }

    @ExceptionHandler(MessageNotFoundException.class)
    ProblemDetail handleNotFound(MessageNotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "Message not found", e.getMessage(), "MESSAGE_NOT_FOUND");
    }

    @ExceptionHandler(AudienceNotFoundException.class)
    ProblemDetail handleAudienceNotFound(AudienceNotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "Audience not found", e.getMessage(), e.code());
    }
}
