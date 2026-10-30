package com.tennisplatform.pass.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The payment gateway's keys and where the student comes back to (42-fase27.5, decision 6). The
 * keys come from the server's environment, never from the repository: blank means online payment
 * cannot be switched on.
 */
@Validated
@ConfigurationProperties(prefix = "tennis.pass")
public class PassProperties {

    /** Stripe's secret key: {@code sk_test_...} to try it out, {@code sk_live_...} to charge. */
    private String stripeSecretKey = "";

    /** Signs Stripe's notices ({@code whsec_...}), from the webhook's page in Stripe's dashboard. */
    private String stripeWebhookSecret = "";

    /** Stripe's API. Only the tests point it elsewhere. */
    @NotBlank
    private String stripeApiBaseUrl = "https://api.stripe.com";

    /** Where Stripe's page sends the student back; the page lives in the frontend. */
    @NotBlank
    private String frontendBaseUrl = "http://localhost:3000";

    public String getStripeSecretKey() {
        return stripeSecretKey;
    }

    public void setStripeSecretKey(String stripeSecretKey) {
        this.stripeSecretKey = stripeSecretKey;
    }

    public String getStripeWebhookSecret() {
        return stripeWebhookSecret;
    }

    public void setStripeWebhookSecret(String stripeWebhookSecret) {
        this.stripeWebhookSecret = stripeWebhookSecret;
    }

    public String getStripeApiBaseUrl() {
        return stripeApiBaseUrl;
    }

    public void setStripeApiBaseUrl(String stripeApiBaseUrl) {
        this.stripeApiBaseUrl = stripeApiBaseUrl;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }
}
