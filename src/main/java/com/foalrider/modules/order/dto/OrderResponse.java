package com.foalrider.modules.order.dto;

import com.foalrider.modules.order.entity.OrderStatus;
import com.foalrider.modules.order.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private UUID id;
    private String orderNumber;
    private OrderStatus status;
    private PaymentStatus paymentStatus;

    // User info
    private UUID userId;
    private String userEmail;
    private String userName;

    // Amounts
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    // Shipping info
    private ShippingAddressResponse shippingAddress;

    // Payment info
    private String paymentMethod;
    private Instant paidAt;

    // Shipping tracking
    private String shippingMethod;
    private String trackingNumber;
    private Instant shippedAt;
    private Instant deliveredAt;

    // Notes
    private String customerNotes;
    private String couponCode;

    // Items
    private List<OrderItemResponse> items;
    private Integer itemCount;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
