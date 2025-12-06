package com.foalrider.modules.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating notification preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationPreferenceRequest {

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

    // Newsletter
    private Boolean newsletterEmail;
}
