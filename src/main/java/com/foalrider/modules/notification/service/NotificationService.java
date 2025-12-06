package com.foalrider.modules.notification.service;

import com.foalrider.modules.notification.dto.*;
import com.foalrider.modules.notification.entity.NotificationType;
import com.foalrider.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for notification operations.
 */
public interface NotificationService {

    /**
     * Create a notification for a user.
     */
    NotificationResponse createNotification(CreateNotificationRequest request);

    /**
     * Create a notification (simplified).
     */
    NotificationResponse sendNotification(UUID userId, NotificationType type, String title, String message);

    /**
     * Create a notification with action URL.
     */
    NotificationResponse sendNotification(UUID userId, NotificationType type, String title, String message, 
                                         String actionUrl, Map<String, Object> metadata);

    /**
     * Get notifications for a user.
     */
    PagedResponse<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable);

    /**
     * Get unread notifications for a user.
     */
    PagedResponse<NotificationResponse> getUnreadNotifications(UUID userId, Pageable pageable);

    /**
     * Get recent notifications (top 10).
     */
    List<NotificationResponse> getRecentNotifications(UUID userId);

    /**
     * Get unread count for a user.
     */
    NotificationCountResponse getUnreadCount(UUID userId);

    /**
     * Mark notification as read.
     */
    NotificationResponse markAsRead(UUID notificationId, UUID userId);

    /**
     * Mark multiple notifications as read.
     */
    int markAsRead(List<UUID> notificationIds, UUID userId);

    /**
     * Mark all notifications as read.
     */
    int markAllAsRead(UUID userId);

    /**
     * Delete a notification.
     */
    void deleteNotification(UUID notificationId, UUID userId);

    /**
     * Get notification preferences.
     */
    NotificationPreferenceResponse getPreferences(UUID userId);

    /**
     * Update notification preferences.
     */
    NotificationPreferenceResponse updatePreferences(UUID userId, UpdateNotificationPreferenceRequest request);

    // Admin methods

    /**
     * Send bulk notification to all users or filtered by role.
     */
    int sendBulkNotification(BulkNotificationRequest request);

    /**
     * Clean up old notifications.
     */
    int cleanupOldNotifications(int daysOld);

    /**
     * Delete expired notifications.
     */
    int deleteExpiredNotifications();

    // Helper methods for specific notification types

    /**
     * Send order status notification.
     */
    void sendOrderStatusNotification(UUID userId, UUID orderId, String orderNumber, String status);

    /**
     * Send payment notification.
     */
    void sendPaymentNotification(UUID userId, UUID orderId, String orderNumber, boolean success);

    /**
     * Send review status notification.
     */
    void sendReviewStatusNotification(UUID userId, UUID reviewId, UUID productId, String productName, boolean approved);

    /**
     * Send welcome notification.
     */
    void sendWelcomeNotification(UUID userId, String userName);
}
