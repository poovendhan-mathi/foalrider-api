package com.foalrider.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for sales report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    // Summary
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private Long totalItemsSold;

    // Daily breakdown
    private List<DailySales> dailySales;

    // Top products
    private List<TopProduct> topProducts;

    // Order status distribution
    private Map<String, Long> orderStatusDistribution;

    // Payment method distribution
    private Map<String, Long> paymentMethodDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySales {
        private LocalDate date;
        private Long orderCount;
        private BigDecimal revenue;
        private Long itemsSold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProduct {
        private String productId;
        private String productName;
        private Long quantitySold;
        private BigDecimal revenue;
    }
}
