package com.tennisplatform.pass.adapters.in.web;

import com.tennisplatform.pass.application.port.in.ConfirmPayments;
import com.tennisplatform.pass.configuration.PassProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

/**
 * Stripe's notices about the payments of passes (42-fase27.5, decision 3). Public: Stripe has no
 * session. What protects it is the signature, checked before anything is read.
 *
 * <p>Only the outcome of a Checkout page matters here. A completed page whose payment is still
 * on its way waits for {@code async_payment_succeeded}; any other event is acknowledged and left
 * alone, so Stripe does not retry it.
 */
@RestController
@RequestMapping("/api/v1")
class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final ConfirmPayments payments;
    private final PassProperties properties;
    private final ObjectMapper json;
    private final Clock clock;

    StripeWebhookController(ConfirmPayments payments, PassProperties properties, ObjectMapper json, Clock clock) {
        this.payments = payments;
        this.properties = properties;
        this.json = json;
        this.clock = clock;
    }

    @PostMapping("/payments/stripe/webhook")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void notice(@RequestBody(required = false) byte[] payload,
                @RequestHeader(name = "Stripe-Signature", required = false) String signature) {
        if (payload == null
                || !StripeSignature.valid(payload, signature, properties.getStripeWebhookSecret(), clock.instant())) {
            throw new InvalidWebhookException("The notice is not signed by Stripe with this server's secret");
        }
        JsonNode event;
        try {
            event = json.readTree(payload);
        } catch (JacksonException e) {
            throw new InvalidWebhookException("The notice is not JSON");
        }
        String type = event.path("type").asString("");
        JsonNode session = event.path("data").path("object");
        String sessionId = session.path("id").asString("");
        switch (type) {
            case "checkout.session.completed" -> {
                if ("paid".equals(session.path("payment_status").asString(""))) {
                    paid(session, sessionId);
                }
            }
            case "checkout.session.async_payment_succeeded" -> paid(session, sessionId);
            case "checkout.session.expired", "checkout.session.async_payment_failed" -> payments.expired(sessionId);
            default -> log.debug("Stripe event {} ignored", type);
        }
    }

    private void paid(JsonNode session, String sessionId) {
        payments.paid(sessionId, session.path("amount_total").asLong(-1), session.path("currency").asString(""));
    }
}
