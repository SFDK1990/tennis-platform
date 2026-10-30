package com.tennisplatform.pass.adapters.in.web;

/** A notice that does not carry a valid signature, or is not what Stripe sends. Maps to 400. */
class InvalidWebhookException extends RuntimeException {

    InvalidWebhookException(String message) {
        super(message);
    }
}
