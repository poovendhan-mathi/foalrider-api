package com.foalrider.modules.admin.controller;

import com.foalrider.modules.admin.dto.*;
import com.foalrider.modules.admin.service.AdminDashboardService;
import com.foalrider.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for admin dashboard and reporting.
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Dashboard", description = "Admin dashboard and reporting APIs")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Get dashboard overview", description = "Get key metrics overview for admin dashboard")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getDashboardOverview() {
        DashboardOverviewResponse overview = dashboardService.getDashboardOverview();
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/sales-report")
    @Operation(summary = "Get sales report", description = "Get detailed sales report for date range")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        SalesReportResponse report = dashboardService.getSalesReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/user-analytics")
    @Operation(summary = "Get user analytics", description = "Get user analytics and statistics")
    public ResponseEntity<ApiResponse<UserAnalyticsResponse>> getUserAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UserAnalyticsResponse analytics = dashboardService.getUserAnalytics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/product-analytics")
    @Operation(summary = "Get product analytics", description = "Get product performance analytics")
    public ResponseEntity<ApiResponse<ProductAnalyticsResponse>> getProductAnalytics() {
        ProductAnalyticsResponse analytics = dashboardService.getProductAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/inventory-report")
    @Operation(summary = "Get inventory report", description = "Get inventory status report")
    public ResponseEntity<ApiResponse<InventoryReportResponse>> getInventoryReport(
            @RequestParam(required = false, defaultValue = "10") Integer lowStockThreshold) {
        InventoryReportResponse report = dashboardService.getInventoryReport(lowStockThreshold);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/recent-activities")
    @Operation(summary = "Get recent activities", description = "Get recent activities feed")
    public ResponseEntity<ApiResponse<List<RecentActivityResponse>>> getRecentActivities(
            @RequestParam(defaultValue = "20") int limit) {
        List<RecentActivityResponse> activities = dashboardService.getRecentActivities(limit);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
}
