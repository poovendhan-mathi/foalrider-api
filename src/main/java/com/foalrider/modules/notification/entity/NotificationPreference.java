package com.foalrider.modules.notification.entity;

import com.foalrider.modules.user.entity.User;
import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * User notification preferences entity.
 */
@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
    @UniqueConstraint(name = "uk_notification_pref_user", columnNames = {"user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Order notifications
    @Column(name = "order_updates_email")
    @Builder.Default
    private Boolean orderUpdatesEmail = true;

    @Column(name = "order_updates_push")
    @Builder.Default
    private Boolean orderUpdatesPush = true;

    @Column(name = "order_updates_sms")
    @Builder.Default
    private Boolean orderUpdatesSms = false;

    // Promotional notifications
    @Column(name = "promotions_email")
    @Builder.Default
    private Boolean promotionsEmail = true;

    @Column(name = "promotions_push")
    @Builder.Default
    private Boolean promotionsPush = false;

    // Price drop alerts
    @Column(name = "price_alerts_email")
    @Builder.Default
    private Boolean priceAlertsEmail = true;

    @Column(name = "price_alerts_push")
    @Builder.Default
    private Boolean priceAlertsPush = true;

    // Stock alerts
    @Column(name = "stock_alerts_email")
    @Builder.Default
    private Boolean stockAlertsEmail = true;

    @Column(name = "stock_alerts_push")
    @Builder.Default
    private Boolean stockAlertsPush = true;

    // Review notifications
    @Column(name = "review_updates_email")
    @Builder.Default
    private Boolean reviewUpdatesEmail = true;

    @Column(name = "review_updates_push")
    @Builder.Default
    private Boolean reviewUpdatesPush = false;

    // Security notifications (always enabled)
    @Column(name = "security_alerts_email")
    @Builder.Default
    private Boolean securityAlertsEmail = true;

    // Newsletter
    @Column(name = "newsletter_email")
    @Builder.Default
    private Boolean newsletterEmail = false;
}
