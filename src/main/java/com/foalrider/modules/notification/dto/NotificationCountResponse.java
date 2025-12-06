package com.foalrider.modules.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for notification count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCountResponse {

    private Long unreadCount;
    private Boolean hasUnread;
}
