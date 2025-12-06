package com.foalrider.modules.notification.dto;

import com.foalrider.modules.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Request DTO for sending bulk notifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationRequest {

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    @Size(max = 500, message = "Action URL cannot exceed 500 characters")
    private String actionUrl;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    private Map<String, Object> metadata;

    private Instant expiresAt;

    // Target criteria
    private Boolean allUsers;
    private String targetRole; // ROLE_CUSTOMER, ROLE_VENDOR, ROLE_ADMIN
}
