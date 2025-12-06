package com.foalrider.modules.order.entity;

import com.foalrider.modules.user.entity.User;
import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Order entity - represents a customer order.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number", columnList = "order_number"),
    @Index(name = "idx_order_user", columnList = "user_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_payment_status", columnList = "payment_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // Amounts
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "shipping_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Shipping information
    @Column(name = "shipping_name", length = 100)
    private String shippingName;

    @Column(name = "shipping_phone", length = 20)
    private String shippingPhone;

    @Column(name = "shipping_email", length = 255)
    private String shippingEmail;

    @Column(name = "shipping_address_line1", length = 255)
    private String shippingAddressLine1;

    @Column(name = "shipping_address_line2", length = 255)
    private String shippingAddressLine2;

    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Column(name = "shipping_state", length = 100)
    private String shippingState;

    @Column(name = "shipping_postal_code", length = 20)
    private String shippingPostalCode;

    @Column(name = "shipping_country", length = 100)
    private String shippingCountry;

    // Billing information (if different from shipping)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "billing_address", columnDefinition = "jsonb")
    private Map<String, String> billingAddress;

    // Payment details
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_intent_id", length = 100)
    private String paymentIntentId;

    @Column(name = "payment_transaction_id", length = 100)
    private String paymentTransactionId;

    @Column(name = "paid_at")
    private Instant paidAt;

    // Shipping tracking
    @Column(name = "shipping_method", length = 50)
    private String shippingMethod;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    // Notes
    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    // Promo/Coupon
    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    // Order items
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // Status history
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    // Helper methods
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void addStatusHistory(OrderStatus newStatus, String note) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(this)
                .status(newStatus)
                .note(note)
                .build();
        statusHistory.add(history);
    }

    public void calculateTotals() {
        this.subtotal = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = subtotal
                .add(taxAmount)
                .add(shippingAmount)
                .subtract(discountAmount);
    }
}
