package com.foalrider.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Stripe payment gateway configuration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "stripe")
public class StripeConfig {

    /**
     * Stripe API secret key.
     */
    private String apiKey;

    /**
     * Stripe webhook signing secret.
     */
    private String webhookSecret;

    /**
     * Default currency for payments.
     */
    private String currency = "usd";

    /**
     * Initialize Stripe with API key on startup.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = this.apiKey;
    }
}
