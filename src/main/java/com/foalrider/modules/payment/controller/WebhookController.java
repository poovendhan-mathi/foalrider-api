package com.foalrider.modules.payment.controller;

import com.foalrider.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook controller for Stripe events.
 * This endpoint should be publicly accessible for Stripe to send events.
 */
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Webhook endpoints for external services")
public class WebhookController {

    private final PaymentService paymentService;

    @PostMapping("/stripe")
    @Operation(summary = "Handle Stripe webhook events")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        log.info("Received Stripe webhook");
        
        try {
            paymentService.handleWebhookEvent(payload, signature);
            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Webhook processing failed");
        }
    }
}
