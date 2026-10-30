package com.tennisplatform.pass.domain;

/** The teacher has not switched on selling passes in the app. Maps to 409. */
public class OnlinePaymentDisabledException extends RuntimeException {

    public OnlinePaymentDisabledException(String message) {
        super(message);
    }
}
