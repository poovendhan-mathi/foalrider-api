package com.foalrider.modules.order.dto;

import com.foalrider.modules.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Simplified order response for list views.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {

    private UUID id;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private String firstItemName;
    private String firstItemImage;
    private Instant createdAt;
}
