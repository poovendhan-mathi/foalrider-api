package com.foalrider.modules.notification.entity;

/**
 * Notification type enum.
 */
public enum NotificationType {
    // Order notifications
    ORDER_PLACED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    ORDER_REFUNDED,
    
    // Payment notifications
    PAYMENT_RECEIVED,
    PAYMENT_FAILED,
    REFUND_PROCESSED,
    
    // Review notifications
    REVIEW_APPROVED,
    REVIEW_REJECTED,
    REVIEW_RESPONSE,
    
    // Product notifications
    PRICE_DROP,
    BACK_IN_STOCK,
    LOW_STOCK_ALERT,
    
    // Account notifications
    WELCOME,
    PASSWORD_CHANGED,
    EMAIL_VERIFIED,
    ACCOUNT_DEACTIVATED,
    
    // Promotional
    PROMOTION,
    COUPON_EXPIRING,
    
    // System
    SYSTEM_ANNOUNCEMENT
}
