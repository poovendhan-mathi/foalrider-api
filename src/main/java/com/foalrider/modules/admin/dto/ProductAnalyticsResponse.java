package com.foalrider.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for product analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAnalyticsResponse {

    // Product counts
    private Long totalProducts;
    private Long activeProducts;
    private Long inactiveProducts;
    private Long featuredProducts;

    // Stock status
    private Long inStockProducts;
    private Long outOfStockProducts;
    private Long lowStockProducts;

    // Category distribution
    private Map<String, Long> categoryDistribution;

    // Brand distribution
    private Map<String, Long> brandDistribution;

    // Top performing products
    private List<ProductPerformance> topByRevenue;
    private List<ProductPerformance> topByQuantity;
    private List<ProductPerformance> topByViews;
    private List<ProductPerformance> topByRating;

    // Low performing products
    private List<ProductPerformance> lowPerforming;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPerformance {
        private String productId;
        private String productName;
        private String sku;
        private Long quantitySold;
        private BigDecimal revenue;
        private Integer viewCount;
        private BigDecimal avgRating;
        private Integer reviewCount;
        private Integer stockQuantity;
    }
}
