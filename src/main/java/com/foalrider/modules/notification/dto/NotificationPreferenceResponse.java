package com.foalrider.modules.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for notification preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private UUID id;
    private UUID userId;

    // Order notifications
    private Boolean orderUpdatesEmail;
    private Boolean orderUpdatesPush;
    private Boolean orderUpdatesSms;

    // Promotional notifications
    private Boolean promotionsEmail;
    private Boolean promotionsPush;

    // Price drop alerts
    private Boolean priceAlertsEmail;
    private Boolean priceAlertsPush;

    // Stock alerts
    private Boolean stockAlertsEmail;
    private Boolean stockAlertsPush;

    // Review notifications
    private Boolean reviewUpdatesEmail;
    private Boolean reviewUpdatesPush;

    // Security notifications
    private Boolean securityAlertsEmail;

    // Newsletter
    private Boolean newsletterEmail;
}
