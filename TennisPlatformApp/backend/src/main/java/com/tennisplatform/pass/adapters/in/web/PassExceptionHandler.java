package com.tennisplatform.pass.adapters.in.web;

import com.tennisplatform.error.Problems;
import com.tennisplatform.pass.domain.InvalidPassException;
import com.tennisplatform.pass.domain.OnlinePaymentDisabledException;
import com.tennisplatform.pass.domain.OnlinePaymentUnavailableException;
import com.tennisplatform.pass.domain.PaymentGatewayException;
import com.tennisplatform.pass.domain.PriceTooLowToPayOnlineException;
import com.tennisplatform.pass.domain.PurchaseNotFoundException;
import com.tennisplatform.pass.domain.PassAlreadyUsedException;
import com.tennisplatform.pass.domain.PassNotFoundException;
import com.tennisplatform.pass.domain.PassTypeAlreadyExistsException;
import com.tennisplatform.pass.domain.PassTypeInUseException;
import com.tennisplatform.pass.domain.PassTypeNotFoundException;
import com.tennisplatform.pass.domain.PassTypeRetiredException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps pass failures to Problem Details, with the 409/422 split of 11-contrato-api.md. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class PassExceptionHandler {

    @ExceptionHandler(PassTypeNotFoundException.class)
    ProblemDetail handleTypeNotFound(PassTypeNotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "Pass type not found", e.getMessage(), "PASS_TYPE_NOT_FOUND");
    }

    @ExceptionHandler(PassNotFoundException.class)
    ProblemDetail handleNotFound(PassNotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "Pass not found", e.getMessage(), "PASS_NOT_FOUND");
    }

    @ExceptionHandler(PassTypeInUseException.class)
    ProblemDetail handleTypeInUse(PassTypeInUseException e) {
        return Problems.of(HttpStatus.CONFLICT, "Pass type in use", e.getMessage(), "PASS_TYPE_IN_USE");
    }

    @ExceptionHandler(PassTypeAlreadyExistsException.class)
    ProblemDetail handleTypeExists(PassTypeAlreadyExistsException e) {
        return Problems.of(HttpStatus.CONFLICT, "Pass type already exists", e.getMessage(),
                "PASS_TYPE_ALREADY_EXISTS");
    }

    @ExceptionHandler(PassAlreadyUsedException.class)
    ProblemDetail handleAlreadyUsed(PassAlreadyUsedException e) {
        return Problems.of(HttpStatus.CONFLICT, "Pass already used", e.getMessage(), "PASS_ALREADY_USED");
    }

    @ExceptionHandler(PassTypeRetiredException.class)
    ProblemDetail handleRetired(PassTypeRetiredException e) {
        return Problems.of(HttpStatus.UNPROCESSABLE_CONTENT, "Pass type retired", e.getMessage(),
                "PASS_TYPE_RETIRED");
    }

    @ExceptionHandler(PurchaseNotFoundException.class)
    ProblemDetail handlePurchaseNotFound(PurchaseNotFoundException e) {
        return Problems.of(HttpStatus.NOT_FOUND, "Purchase not found", e.getMessage(), "PURCHASE_NOT_FOUND");
    }

    @ExceptionHandler(OnlinePaymentDisabledException.class)
    ProblemDetail handleOnlinePaymentDisabled(OnlinePaymentDisabledException e) {
        return Problems.of(HttpStatus.CONFLICT, "Online payment switched off", e.getMessage(),
                "ONLINE_PAYMENT_DISABLED");
    }

    @ExceptionHandler(OnlinePaymentUnavailableException.class)
    ProblemDetail handleOnlinePaymentUnavailable(OnlinePaymentUnavailableException e) {
        return Problems.of(HttpStatus.CONFLICT, "Online payment unavailable", e.getMessage(),
                "ONLINE_PAYMENT_UNAVAILABLE");
    }

    @ExceptionHandler(PriceTooLowToPayOnlineException.class)
    ProblemDetail handlePriceTooLow(PriceTooLowToPayOnlineException e) {
        return Problems.of(HttpStatus.UNPROCESSABLE_CONTENT, "Price too low to pay online", e.getMessage(),
                "PRICE_TOO_LOW_TO_PAY_ONLINE");
    }

    @ExceptionHandler(PaymentGatewayException.class)
    ProblemDetail handleGateway(PaymentGatewayException e) {
        return Problems.of(HttpStatus.SERVICE_UNAVAILABLE, "Payment gateway unavailable", e.getMessage(),
                "PAYMENT_GATEWAY_UNAVAILABLE");
    }

    @ExceptionHandler(InvalidWebhookException.class)
    ProblemDetail handleInvalidWebhook(InvalidWebhookException e) {
        return Problems.of(HttpStatus.BAD_REQUEST, "Invalid notice", e.getMessage(), "INVALID_WEBHOOK_SIGNATURE");
    }

    @ExceptionHandler(InvalidPassException.class)
    ProblemDetail handleInvalid(InvalidPassException e) {
        return Problems.of(HttpStatus.BAD_REQUEST, "Invalid pass", e.getMessage(), "VALIDATION_ERROR");
    }
}
