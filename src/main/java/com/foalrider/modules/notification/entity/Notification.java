package com.foalrider.modules.notification.entity;

import com.foalrider.modules.user.entity.User;
import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Notification entity for user notifications.
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "user_id"),
    @Index(name = "idx_notification_type", columnList = "type"),
    @Index(name = "idx_notification_read", columnList = "is_read"),
    @Index(name = "idx_notification_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.IN_APP;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Mark notification as read.
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = Instant.now();
    }

    /**
     * Check if notification is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
