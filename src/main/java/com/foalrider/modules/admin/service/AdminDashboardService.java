package com.foalrider.modules.admin.service;

import com.foalrider.modules.admin.dto.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for admin dashboard and reporting.
 */
public interface AdminDashboardService {

    /**
     * Get dashboard overview with key metrics.
     */
    DashboardOverviewResponse getDashboardOverview();

    /**
     * Get sales report for date range.
     */
    SalesReportResponse getSalesReport(LocalDate startDate, LocalDate endDate);

    /**
     * Get user analytics for date range.
     */
    UserAnalyticsResponse getUserAnalytics(LocalDate startDate, LocalDate endDate);

    /**
     * Get product analytics.
     */
    ProductAnalyticsResponse getProductAnalytics();

    /**
     * Get inventory report.
     */
    InventoryReportResponse getInventoryReport(Integer lowStockThreshold);

    /**
     * Get recent activities.
     */
    List<RecentActivityResponse> getRecentActivities(int limit);
}
