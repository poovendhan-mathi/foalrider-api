package com.foalrider.modules.notification.service;

import com.foalrider.modules.notification.dto.*;
import com.foalrider.modules.notification.entity.*;
import com.foalrider.modules.notification.repository.NotificationPreferenceRepository;
import com.foalrider.modules.notification.repository.NotificationRepository;
import com.foalrider.modules.user.entity.User;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.shared.dto.PagedResponse;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.foalrider.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Override
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .actionUrl(request.getActionUrl())
                .imageUrl(request.getImageUrl())
                .metadata(request.getMetadata())
                .expiresAt(request.getExpiresAt())
                .channel(NotificationChannel.IN_APP)
                .sentAt(Instant.now())
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created for user {}: {}", request.getUserId(), request.getType());

        return mapToResponse(notification);
    }

    @Override
    public NotificationResponse sendNotification(UUID userId, NotificationType type, String title, String message) {
        return sendNotification(userId, type, title, message, null, null);
    }

    @Override
    public NotificationResponse sendNotification(UUID userId, NotificationType type, String title, String message,
                                                 String actionUrl, Map<String, Object> metadata) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .metadata(metadata)
                .build();

        return createNotification(request);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return mapToPagedResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUnreadNotifications(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        return mapToPagedResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(UUID userId) {
        return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationCountResponse getUnreadCount(UUID userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        boolean hasUnread = count > 0;

        return NotificationCountResponse.builder()
                .unreadCount(count)
                .hasUnread(hasUnread)
                .build();
    }

    @Override
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only mark your own notifications as read");
        }

        notification.markAsRead();
        notification = notificationRepository.save(notification);

        return mapToResponse(notification);
    }

    @Override
    public int markAsRead(List<UUID> notificationIds, UUID userId) {
        return notificationRepository.markAsRead(notificationIds, userId, Instant.now());
    }

    @Override
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsRead(userId, Instant.now());
    }

    @Override
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own notifications");
        }

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(UUID userId) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        return mapPreferenceToResponse(preference);
    }

    @Override
    public NotificationPreferenceResponse updatePreferences(UUID userId, UpdateNotificationPreferenceRequest request) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        // Update non-null fields
        if (request.getOrderUpdatesEmail() != null) {
            preference.setOrderUpdatesEmail(request.getOrderUpdatesEmail());
        }
        if (request.getOrderUpdatesPush() != null) {
            preference.setOrderUpdatesPush(request.getOrderUpdatesPush());
        }
        if (request.getOrderUpdatesSms() != null) {
            preference.setOrderUpdatesSms(request.getOrderUpdatesSms());
        }
        if (request.getPromotionsEmail() != null) {
            preference.setPromotionsEmail(request.getPromotionsEmail());
        }
        if (request.getPromotionsPush() != null) {
            preference.setPromotionsPush(request.getPromotionsPush());
        }
        if (request.getPriceAlertsEmail() != null) {
            preference.setPriceAlertsEmail(request.getPriceAlertsEmail());
        }
        if (request.getPriceAlertsPush() != null) {
            preference.setPriceAlertsPush(request.getPriceAlertsPush());
        }
        if (request.getStockAlertsEmail() != null) {
            preference.setStockAlertsEmail(request.getStockAlertsEmail());
        }
        if (request.getStockAlertsPush() != null) {
            preference.setStockAlertsPush(request.getStockAlertsPush());
        }
        if (request.getReviewUpdatesEmail() != null) {
            preference.setReviewUpdatesEmail(request.getReviewUpdatesEmail());
        }
        if (request.getReviewUpdatesPush() != null) {
            preference.setReviewUpdatesPush(request.getReviewUpdatesPush());
        }
        if (request.getNewsletterEmail() != null) {
            preference.setNewsletterEmail(request.getNewsletterEmail());
        }

        preference = preferenceRepository.save(preference);
        log.info("Notification preferences updated for user {}", userId);

        return mapPreferenceToResponse(preference);
    }

    // Admin methods

    @Override
    public int sendBulkNotification(BulkNotificationRequest request) {
        List<User> targetUsers;

        if (Boolean.TRUE.equals(request.getAllUsers())) {
            targetUsers = userRepository.findAll();
        } else if (request.getTargetRole() != null) {
            targetUsers = userRepository.findByRoleName(request.getTargetRole());
        } else {
            log.warn("No target specified for bulk notification");
            return 0;
        }

        int count = 0;
        for (User user : targetUsers) {
            try {
                Notification notification = Notification.builder()
                        .user(user)
                        .type(request.getType())
                        .title(request.getTitle())
                        .message(request.getMessage())
                        .actionUrl(request.getActionUrl())
                        .imageUrl(request.getImageUrl())
                        .metadata(request.getMetadata())
                        .expiresAt(request.getExpiresAt())
                        .channel(NotificationChannel.IN_APP)
                        .sentAt(Instant.now())
                        .build();
                
                notificationRepository.save(notification);
                count++;
            } catch (Exception e) {
                log.error("Failed to send notification to user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Bulk notification sent to {} users", count);
        return count;
    }

    @Override
    public int cleanupOldNotifications(int daysOld) {
        Instant cutoffDate = Instant.now().minus(daysOld, ChronoUnit.DAYS);
        int deleted = notificationRepository.deleteOldReadNotifications(cutoffDate);
        log.info("Cleaned up {} old notifications", deleted);
        return deleted;
    }

    @Override
    public int deleteExpiredNotifications() {
        int deleted = notificationRepository.deleteExpiredNotifications(Instant.now());
        log.info("Deleted {} expired notifications", deleted);
        return deleted;
    }

    // Helper notification methods

    @Override
    @Async
    public void sendOrderStatusNotification(UUID userId, UUID orderId, String orderNumber, String status) {
        String title = "Order " + status;
        String message = String.format("Your order #%s has been %s.", orderNumber, status.toLowerCase());
        String actionUrl = "/orders/" + orderId;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderId", orderId.toString());
        metadata.put("orderNumber", orderNumber);
        metadata.put("status", status);

        NotificationType type = switch (status.toUpperCase()) {
            case "CONFIRMED" -> NotificationType.ORDER_CONFIRMED;
            case "SHIPPED" -> NotificationType.ORDER_SHIPPED;
            case "DELIVERED" -> NotificationType.ORDER_DELIVERED;
            case "CANCELLED" -> NotificationType.ORDER_CANCELLED;
            default -> NotificationType.ORDER_PLACED;
        };

        sendNotification(userId, type, title, message, actionUrl, metadata);
    }

    @Override
    @Async
    public void sendPaymentNotification(UUID userId, UUID orderId, String orderNumber, boolean success) {
        String title = success ? "Payment Successful" : "Payment Failed";
        String message = success 
                ? String.format("Payment for order #%s has been received.", orderNumber)
                : String.format("Payment for order #%s has failed. Please try again.", orderNumber);
        String actionUrl = "/orders/" + orderId;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderId", orderId.toString());
        metadata.put("orderNumber", orderNumber);

        NotificationType type = success ? NotificationType.PAYMENT_RECEIVED : NotificationType.PAYMENT_FAILED;

        sendNotification(userId, type, title, message, actionUrl, metadata);
    }

    @Override
    @Async
    public void sendReviewStatusNotification(UUID userId, UUID reviewId, UUID productId, String productName, boolean approved) {
        String title = approved ? "Review Approved" : "Review Not Approved";
        String message = approved
                ? String.format("Your review for \"%s\" has been approved and is now visible.", productName)
                : String.format("Your review for \"%s\" did not meet our guidelines.", productName);
        String actionUrl = "/products/" + productId;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reviewId", reviewId.toString());
        metadata.put("productId", productId.toString());

        NotificationType type = approved ? NotificationType.REVIEW_APPROVED : NotificationType.REVIEW_REJECTED;

        sendNotification(userId, type, title, message, actionUrl, metadata);
    }

    @Override
    @Async
    public void sendWelcomeNotification(UUID userId, String userName) {
        String title = "Welcome to FoalRider!";
        String message = String.format("Hi %s! Welcome to FoalRider. Start exploring our products and enjoy shopping!", userName);
        String actionUrl = "/products";

        sendNotification(userId, NotificationType.WELCOME, title, message, actionUrl, null);
    }

    // Private helper methods

    private NotificationPreference createDefaultPreferences(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        NotificationPreference preference = NotificationPreference.builder()
                .user(user)
                .build();

        return preferenceRepository.save(preference);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .actionUrl(notification.getActionUrl())
                .imageUrl(notification.getImageUrl())
                .metadata(notification.getMetadata())
                .channel(notification.getChannel())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private NotificationPreferenceResponse mapPreferenceToResponse(NotificationPreference pref) {
        return NotificationPreferenceResponse.builder()
                .id(pref.getId())
                .userId(pref.getUser().getId())
                .orderUpdatesEmail(pref.getOrderUpdatesEmail())
                .orderUpdatesPush(pref.getOrderUpdatesPush())
                .orderUpdatesSms(pref.getOrderUpdatesSms())
                .promotionsEmail(pref.getPromotionsEmail())
                .promotionsPush(pref.getPromotionsPush())
                .priceAlertsEmail(pref.getPriceAlertsEmail())
                .priceAlertsPush(pref.getPriceAlertsPush())
                .stockAlertsEmail(pref.getStockAlertsEmail())
                .stockAlertsPush(pref.getStockAlertsPush())
                .reviewUpdatesEmail(pref.getReviewUpdatesEmail())
                .reviewUpdatesPush(pref.getReviewUpdatesPush())
                .securityAlertsEmail(pref.getSecurityAlertsEmail())
                .newsletterEmail(pref.getNewsletterEmail())
                .build();
    }

    private PagedResponse<NotificationResponse> mapToPagedResponse(Page<Notification> page) {
        List<NotificationResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<NotificationResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
