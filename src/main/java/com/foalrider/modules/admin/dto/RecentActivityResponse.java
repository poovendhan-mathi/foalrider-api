package com.foalrider.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for recent activity items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityResponse {

    private UUID id;
    private ActivityType type;
    private String title;
    private String description;
    private String actorName;
    private String actorEmail;
    private UUID actorId;
    private String metadata;
    private Instant timestamp;

    public enum ActivityType {
        NEW_ORDER,
        ORDER_STATUS_CHANGE,
        PAYMENT_RECEIVED,
        NEW_USER,
        NEW_REVIEW,
        PRODUCT_CREATED,
        PRODUCT_UPDATED,
        LOW_STOCK_ALERT,
        REFUND_PROCESSED
    }
}
