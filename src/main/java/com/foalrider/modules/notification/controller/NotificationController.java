package com.foalrider.modules.notification.controller;

import com.foalrider.modules.notification.dto.*;
import com.foalrider.modules.notification.service.NotificationService;
import com.foalrider.security.SecurityUtils;
import com.foalrider.shared.dto.ApiResponse;
import com.foalrider.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for notification operations.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification management")
public class NotificationController {

    private final NotificationService notificationService;

    // ==================== USER ENDPOINTS ====================

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get notifications", description = "Get paginated notifications for current user")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        UUID userId = SecurityUtils.requireCurrentUserId();
        PagedResponse<NotificationResponse> notifications = notificationService.getUserNotifications(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread notifications", description = "Get unread notifications for current user")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        UUID userId = SecurityUtils.requireCurrentUserId();
        PagedResponse<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get recent notifications", description = "Get 10 most recent notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getRecentNotifications() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        List<NotificationResponse> notifications = notificationService.getRecentNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread count", description = "Get count of unread notifications")
    public ResponseEntity<ApiResponse<NotificationCountResponse>> getUnreadCount() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        NotificationCountResponse count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark as read", description = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable UUID notificationId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        NotificationResponse notification = notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success(notification, "Notification marked as read"));
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(count, "All notifications marked as read"));
    }

    @PutMapping("/read-batch")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark batch as read", description = "Mark multiple notifications as read")
    public ResponseEntity<ApiResponse<Integer>> markBatchAsRead(@RequestBody List<UUID> notificationIds) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        int count = notificationService.markAsRead(notificationIds, userId);
        return ResponseEntity.ok(ApiResponse.success(count, count + " notifications marked as read"));
    }

    @DeleteMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete notification", description = "Delete a notification")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable UUID notificationId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted"));
    }

    // ==================== PREFERENCES ====================

    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get preferences", description = "Get notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreferences() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        NotificationPreferenceResponse preferences = notificationService.getPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    @PutMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update preferences", description = "Update notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        NotificationPreferenceResponse preferences = notificationService.updatePreferences(userId, request);
        return ResponseEntity.ok(ApiResponse.success(preferences, "Preferences updated"));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @PostMapping("/admin/send")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send notification (Admin)", description = "Send notification to a specific user")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        NotificationResponse notification = notificationService.createNotification(request);
        return ResponseEntity.ok(ApiResponse.success(notification, "Notification sent"));
    }

    @PostMapping("/admin/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send bulk notification (Admin)", description = "Send notification to multiple users")
    public ResponseEntity<ApiResponse<Integer>> sendBulkNotification(
            @Valid @RequestBody BulkNotificationRequest request) {
        int count = notificationService.sendBulkNotification(request);
        return ResponseEntity.ok(ApiResponse.success(count, "Sent to " + count + " users"));
    }

    @PostMapping("/admin/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cleanup old notifications (Admin)", description = "Delete old read notifications")
    public ResponseEntity<ApiResponse<Integer>> cleanupNotifications(
            @RequestParam(defaultValue = "30") int daysOld) {
        int count = notificationService.cleanupOldNotifications(daysOld);
        return ResponseEntity.ok(ApiResponse.success(count, "Cleaned up " + count + " notifications"));
    }
}
