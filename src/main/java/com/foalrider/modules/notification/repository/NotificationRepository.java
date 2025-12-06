package com.foalrider.modules.notification.repository;

import com.foalrider.modules.notification.entity.Notification;
import com.foalrider.modules.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Notification entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find notifications by user ID, ordered by creation date.
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find unread notifications by user ID.
     */
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find notifications by user ID and type.
     */
    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, NotificationType type, Pageable pageable);

    /**
     * Count unread notifications for a user.
     */
    long countByUserIdAndIsReadFalse(UUID userId);

    /**
     * Mark all notifications as read for a user.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") Instant readAt);

    /**
     * Mark specific notifications as read.
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.id IN :ids AND n.user.id = :userId")
    int markAsRead(@Param("ids") List<UUID> ids, @Param("userId") UUID userId, @Param("readAt") Instant readAt);

    /**
     * Delete old read notifications.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.isRead = true AND n.createdAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Delete expired notifications.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    int deleteExpiredNotifications(@Param("now") Instant now);

    /**
     * Find recent notifications by user (limit).
     */
    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Check if user has unread notifications.
     */
    boolean existsByUserIdAndIsReadFalse(UUID userId);
}
