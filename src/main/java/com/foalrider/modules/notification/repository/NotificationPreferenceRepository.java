package com.foalrider.modules.notification.repository;

import com.foalrider.modules.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for NotificationPreference entity.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    /**
     * Find preference by user ID.
     */
    Optional<NotificationPreference> findByUserId(UUID userId);

    /**
     * Check if preference exists for user.
     */
    boolean existsByUserId(UUID userId);
}
