package com.tennisplatform.pass.domain;

/** The gateway charges at least 0,50 euros, so a cheaper type cannot be bought in the app. Maps to 422. */
public class PriceTooLowToPayOnlineException extends RuntimeException {

    public PriceTooLowToPayOnlineException(String message) {
        super(message);
    }
}
