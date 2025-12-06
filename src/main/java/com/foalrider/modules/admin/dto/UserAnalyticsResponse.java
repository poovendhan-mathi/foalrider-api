package com.foalrider.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for user analytics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnalyticsResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    // User counts
    private Long totalUsers;
    private Long activeUsers;
    private Long inactiveUsers;
    private Long newUsers;

    // Registration trend
    private List<DailyRegistration> registrationTrend;

    // Role distribution
    private Map<String, Long> roleDistribution;

    // Top customers by orders
    private List<TopCustomer> topCustomersByOrders;

    // Top customers by revenue
    private List<TopCustomer> topCustomersByRevenue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRegistration {
        private LocalDate date;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopCustomer {
        private String userId;
        private String name;
        private String email;
        private Long orderCount;
        private java.math.BigDecimal totalSpent;
    }
}
