package com.foalrider.modules.order.entity;

/**
 * Order status enum.
 */
public enum OrderStatus {
    PENDING,        // Order created, awaiting payment
    CONFIRMED,      // Payment received, order confirmed
    PROCESSING,     // Order being prepared
    SHIPPED,        // Order shipped
    DELIVERED,      // Order delivered
    COMPLETED,      // Order completed (after delivery)
    CANCELLED,      // Order cancelled
    REFUNDED,       // Order refunded
    FAILED          // Order failed (payment failed, etc.)
}
