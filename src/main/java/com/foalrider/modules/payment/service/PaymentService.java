package com.foalrider.modules.payment.service;

import com.foalrider.modules.payment.dto.PaymentIntentResponse;

import java.util.UUID;

public interface PaymentService {

    /**
     * Create a Stripe payment intent for an order.
     */
    PaymentIntentResponse createPaymentIntent(UUID orderId);

    /**
     * Confirm a payment (mark order as paid).
     */
    void confirmPayment(String paymentIntentId);

    /**
     * Handle Stripe webhook events.
     */
    void handleWebhookEvent(String payload, String signature);

    /**
     * Process refund for an order.
     */
    void processRefund(UUID orderId, String reason);
}
