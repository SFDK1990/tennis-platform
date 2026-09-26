package com.tennisplatform.administration.adapters.in.web;

import com.tennisplatform.administration.domain.AccountNotFoundException;
import com.tennisplatform.error.Problems;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class AdministrationExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail accountNotFound(AccountNotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "Account not found", e.getMessage(), "USER_NOT_FOUND");
    }
}
