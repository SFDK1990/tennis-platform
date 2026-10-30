package com.tennisplatform.pass.application.port.in;

/**
 * What the gateway says about a payment, once its inbound adapter has checked the signature.
 * Both are safe to repeat: the gateway retries a notice until it is answered.
 */
public interface ConfirmPayments {

    /**
     * The payment went through. Creates the pass, once, if the gateway charged what the purchase
     * asked for; a notice for a purchase the app does not know is ignored.
     */
    void paid(String checkoutSessionId, long amountCents, String currency);

    /** The student left the page without paying. */
    void expired(String checkoutSessionId);
}
