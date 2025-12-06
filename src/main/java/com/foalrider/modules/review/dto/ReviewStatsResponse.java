package com.foalrider.modules.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for review statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsResponse {

    private UUID productId;
    private BigDecimal averageRating;
    private Long totalReviews;
    private Map<Integer, Long> ratingDistribution; // rating (1-5) -> count
    private Long verifiedPurchaseCount;
}
