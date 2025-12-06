package com.foalrider.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response DTO for dashboard overview.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {

    // Orders
    private Long totalOrders;
    private Long pendingOrders;
    private Long processingOrders;
    private Long shippedOrders;
    private Long deliveredOrders;
    private Long cancelledOrders;

    // Revenue
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal weekRevenue;
    private BigDecimal monthRevenue;

    // Users
    private Long totalUsers;
    private Long activeUsers;
    private Long newUsersToday;
    private Long newUsersWeek;
    private Long newUsersMonth;

    // Products
    private Long totalProducts;
    private Long activeProducts;
    private Long outOfStockProducts;
    private Long lowStockProducts;

    // Reviews
    private Long totalReviews;
    private Long pendingReviews;
    private BigDecimal averageRating;

    // Categories
    private Long totalCategories;
    private Long totalBrands;
}
