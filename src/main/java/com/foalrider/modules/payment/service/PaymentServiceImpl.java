package com.foalrider.modules.payment.service;

import com.foalrider.config.StripeConfig;
import com.foalrider.modules.order.entity.Order;
import com.foalrider.modules.order.entity.OrderStatus;
import com.foalrider.modules.order.entity.PaymentStatus;
import com.foalrider.modules.order.repository.OrderRepository;
import com.foalrider.modules.payment.dto.PaymentIntentResponse;
import com.foalrider.security.SecurityUtils;
import com.foalrider.shared.exception.BadRequestException;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.foalrider.shared.exception.UnauthorizedException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final StripeConfig stripeConfig;

    @Override
    public PaymentIntentResponse createPaymentIntent(UUID orderId) {
        UUID userId = getCurrentUserId();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Verify ownership
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }

        // Check order status
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is not in pending status");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid");
        }

        try {
            // Convert amount to cents (Stripe expects smallest currency unit)
            long amountInCents = order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(stripeConfig.getCurrency())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .putMetadata("order_id", order.getId().toString())
                    .putMetadata("order_number", order.getOrderNumber())
                    .putMetadata("user_id", userId.toString())
                    .setDescription("Order " + order.getOrderNumber())
                    .setReceiptEmail(order.getUser().getEmail())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Store payment intent ID on order
            order.setPaymentIntentId(paymentIntent.getId());
            order.setPaymentMethod("stripe");
            order.setPaymentStatus(PaymentStatus.PROCESSING);
            orderRepository.save(order);

            log.info("Created payment intent {} for order {}", paymentIntent.getId(), order.getOrderNumber());

            return PaymentIntentResponse.builder()
                    .clientSecret(paymentIntent.getClientSecret())
                    .paymentIntentId(paymentIntent.getId())
                    .amount(order.getTotalAmount())
                    .currency(stripeConfig.getCurrency())
                    .status(paymentIntent.getStatus())
                    .build();

        } catch (StripeException e) {
            log.error("Failed to create payment intent for order {}: {}", orderId, e.getMessage());
            throw new BadRequestException("Failed to create payment: " + e.getMessage());
        }
    }

    @Override
    public void confirmPayment(String paymentIntentId) {
        Order order = orderRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found for payment intent"));

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaidAt(Instant.now());
        order.addStatusHistory(OrderStatus.CONFIRMED, "Payment confirmed via Stripe");

        orderRepository.save(order);
        log.info("Payment confirmed for order {}", order.getOrderNumber());
    }

    @Override
    public void handleWebhookEvent(String payload, String signature) {
        Event event;
        
        try {
            event = Webhook.constructEvent(payload, signature, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature: {}", e.getMessage());
            throw new BadRequestException("Invalid webhook signature");
        }

        log.info("Received Stripe webhook event: {}", event.getType());

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            log.warn("Unable to deserialize webhook event data");
            return;
        }

        switch (event.getType()) {
            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded((PaymentIntent) stripeObject);
                break;
            case "payment_intent.payment_failed":
                handlePaymentIntentFailed((PaymentIntent) stripeObject);
                break;
            case "charge.refunded":
                log.info("Charge refunded webhook received");
                break;
            default:
                log.info("Unhandled webhook event type: {}", event.getType());
        }
    }

    @Override
    public void processRefund(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Order has not been paid");
        }

        if (order.getPaymentIntentId() == null) {
            throw new BadRequestException("No payment intent found for this order");
        }

        try {
            // Get the payment intent to find the charge
            PaymentIntent paymentIntent = PaymentIntent.retrieve(order.getPaymentIntentId());
            String chargeId = paymentIntent.getLatestCharge();

            if (chargeId == null) {
                throw new BadRequestException("No charge found for this payment");
            }

            RefundCreateParams params = RefundCreateParams.builder()
                    .setCharge(chargeId)
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .putMetadata("order_id", order.getId().toString())
                    .putMetadata("reason", reason != null ? reason : "Customer requested refund")
                    .build();

            Refund refund = Refund.create(params);

            // Update order status
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            order.setStatus(OrderStatus.REFUNDED);
            order.setPaymentTransactionId(refund.getId());
            order.addStatusHistory(OrderStatus.REFUNDED, "Refund processed: " + reason);

            orderRepository.save(order);
            log.info("Refund {} processed for order {}", refund.getId(), order.getOrderNumber());

        } catch (StripeException e) {
            log.error("Failed to process refund for order {}: {}", orderId, e.getMessage());
            throw new BadRequestException("Failed to process refund: " + e.getMessage());
        }
    }

    // Private helper methods

    private UUID getCurrentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));
    }

    private void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        String orderId = paymentIntent.getMetadata().get("order_id");
        if (orderId == null) {
            log.warn("Payment intent {} has no order_id metadata", paymentIntent.getId());
            return;
        }

        try {
            Order order = orderRepository.findById(UUID.fromString(orderId))
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for payment intent {}", paymentIntent.getId());
                return;
            }

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                log.info("Order {} already marked as paid", order.getOrderNumber());
                return;
            }

            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaidAt(Instant.now());
            order.setPaymentTransactionId(paymentIntent.getLatestCharge());
            order.addStatusHistory(OrderStatus.CONFIRMED, "Payment successful via Stripe webhook");

            orderRepository.save(order);
            log.info("Order {} payment confirmed via webhook", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Error processing payment_intent.succeeded for {}: {}", paymentIntent.getId(), e.getMessage());
        }
    }

    private void handlePaymentIntentFailed(PaymentIntent paymentIntent) {
        String orderId = paymentIntent.getMetadata().get("order_id");
        if (orderId == null) {
            log.warn("Payment intent {} has no order_id metadata", paymentIntent.getId());
            return;
        }

        try {
            Order order = orderRepository.findById(UUID.fromString(orderId))
                    .orElse(null);

            if (order == null) {
                log.warn("Order not found for payment intent {}", paymentIntent.getId());
                return;
            }

            order.setPaymentStatus(PaymentStatus.FAILED);
            order.addStatusHistory(order.getStatus(), "Payment failed: " + 
                    (paymentIntent.getLastPaymentError() != null 
                            ? paymentIntent.getLastPaymentError().getMessage() 
                            : "Unknown error"));

            orderRepository.save(order);
            log.info("Order {} payment failed", order.getOrderNumber());

        } catch (Exception e) {
            log.error("Error processing payment_intent.payment_failed for {}: {}", paymentIntent.getId(), e.getMessage());
        }
    }
}
