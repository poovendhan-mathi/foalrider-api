package com.foalrider.modules.notification.dto;

import com.foalrider.modules.notification.entity.NotificationChannel;
import com.foalrider.modules.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for notifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private Boolean isRead;
    private Instant readAt;
    private String actionUrl;
    private String imageUrl;
    private Map<String, Object> metadata;
    private NotificationChannel channel;
    private Instant createdAt;
}
