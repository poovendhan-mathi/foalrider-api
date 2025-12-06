package com.foalrider.modules.order.entity;

/**
 * Payment status enum.
 */
public enum PaymentStatus {
    PENDING,        // Payment not yet initiated
    PROCESSING,     // Payment being processed
    PAID,           // Payment successful
    FAILED,         // Payment failed
    REFUNDED,       // Payment refunded
    PARTIALLY_REFUNDED  // Partial refund issued
}
