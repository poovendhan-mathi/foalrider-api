package com.foalrider.modules.payment.controller;

import com.foalrider.modules.payment.dto.CreatePaymentRequest;
import com.foalrider.modules.payment.dto.PaymentIntentResponse;
import com.foalrider.modules.payment.service.PaymentService;
import com.foalrider.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-intent")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a Stripe payment intent for an order")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> createPaymentIntent(
            @Valid @RequestBody CreatePaymentRequest request) {
        PaymentIntentResponse response = paymentService.createPaymentIntent(request.getOrderId());
        return ResponseEntity.ok(ApiResponse.success(response, "Payment intent created successfully"));
    }

    @PostMapping("/confirm/{paymentIntentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirm a payment (called after client-side payment succeeds)")
    public ResponseEntity<ApiResponse<Void>> confirmPayment(@PathVariable String paymentIntentId) {
        paymentService.confirmPayment(paymentIntentId);
        return ResponseEntity.ok(ApiResponse.success(null, "Payment confirmed successfully"));
    }

    @PostMapping("/refund/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process refund for an order (admin only)")
    public ResponseEntity<ApiResponse<Void>> processRefund(
            @PathVariable UUID orderId,
            @RequestParam(required = false) String reason) {
        paymentService.processRefund(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success(null, "Refund processed successfully"));
    }
}
