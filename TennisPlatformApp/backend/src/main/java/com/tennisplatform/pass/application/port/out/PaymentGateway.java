package com.tennisplatform.pass.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * The payment gateway: Stripe today (42-fase27.5, decision 1). Changing gateway is a new adapter
 * behind this port. What the gateway says back arrives through its signed notice, which its
 * inbound adapter checks before calling {@code ConfirmPayments}.
 */
public interface PaymentGateway {

    /** Whether the gateway has its keys. Without them nothing can be sold online. */
    boolean available();

    /**
     * Opens the gateway's page where the student pays. Raises {@code PaymentGatewayException}
     * when the gateway does not answer or refuses.
     */
    Checkout open(CheckoutRequest request);

    /** {@code purchaseId} travels with the payment and comes back in the gateway's notice. */
    record CheckoutRequest(UUID purchaseId, String description, int amountCents, String currency, Instant expiresAt) {
    }

    /** {@code sessionId} is the gateway's reference for the payment; {@code url}, its page. */
    record Checkout(String sessionId, String url) {
    }
}
