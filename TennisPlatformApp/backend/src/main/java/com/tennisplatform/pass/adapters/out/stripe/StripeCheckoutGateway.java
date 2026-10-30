package com.tennisplatform.pass.adapters.out.stripe;

import com.tennisplatform.pass.application.port.out.PaymentGateway;
import com.tennisplatform.pass.configuration.PassProperties;
import com.tennisplatform.pass.domain.PaymentGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * Opens a Stripe Checkout page: Stripe's own page, so no card or Bizum detail ever passes
 * through the app and the frontend's CSP stays closed to third-party scripts (42-fase27.5).
 * Which methods it offers (card, Apple Pay, Google Pay, Bizum) is set in Stripe's dashboard.
 *
 * <p>The purchase id goes as {@code client_reference_id} and as the idempotency key, so a
 * retried call never opens two pages for one purchase.
 */
@Component
class StripeCheckoutGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripeCheckoutGateway.class);

    private final PassProperties properties;
    private final RestClient client;

    StripeCheckoutGateway(PassProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requests.setReadTimeout(Duration.ofSeconds(20));
        this.client = RestClient.builder()
                .baseUrl(properties.getStripeApiBaseUrl())
                .requestFactory(requests)
                .build();
    }

    @Override
    public boolean available() {
        return !properties.getStripeSecretKey().isBlank() && !properties.getStripeWebhookSecret().isBlank();
    }

    @Override
    public Checkout open(CheckoutRequest request) {
        String back = properties.getFrontendBaseUrl() + "/passes?purchase=" + request.purchaseId();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "payment");
        form.add("client_reference_id", request.purchaseId().toString());
        form.add("metadata[purchase_id]", request.purchaseId().toString());
        form.add("line_items[0][quantity]", "1");
        form.add("line_items[0][price_data][currency]", request.currency());
        form.add("line_items[0][price_data][unit_amount]", Integer.toString(request.amountCents()));
        form.add("line_items[0][price_data][product_data][name]", request.description());
        form.add("success_url", back);
        form.add("cancel_url", back + "&cancelled=true");
        form.add("expires_at", Long.toString(request.expiresAt().getEpochSecond()));
        form.add("locale", "es");
        try {
            Map<?, ?> session = client.post()
                    .uri("/v1/checkout/sessions")
                    .headers(headers -> {
                        headers.setBearerAuth(properties.getStripeSecretKey());
                        headers.set("Idempotency-Key", "purchase-" + request.purchaseId());
                    })
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            if (session == null || !(session.get("id") instanceof String id) || !(session.get("url") instanceof String url)) {
                throw new PaymentGatewayException("Stripe answered without a checkout page");
            }
            return new Checkout(id, url);
        } catch (RestClientResponseException e) {
            log.warn("Stripe refused the checkout of purchase {} with {}", request.purchaseId(), e.getStatusCode().value());
            throw new PaymentGatewayException("Stripe refused to open the payment page", e);
        } catch (RestClientException e) {
            log.warn("Stripe did not answer for purchase {}: {}", request.purchaseId(), e.getClass().getSimpleName());
            throw new PaymentGatewayException("Stripe did not answer", e);
        }
    }
}
