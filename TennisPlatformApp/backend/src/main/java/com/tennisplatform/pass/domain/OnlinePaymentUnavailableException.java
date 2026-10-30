package com.tennisplatform.pass.domain;

/** The payment gateway has no keys, so online payment cannot be switched on. Maps to 409. */
public class OnlinePaymentUnavailableException extends RuntimeException {

    public OnlinePaymentUnavailableException(String message) {
        super(message);
    }
}
