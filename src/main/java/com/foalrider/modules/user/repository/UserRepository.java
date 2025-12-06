package com.foalrider.modules.user.repository;

import com.foalrider.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Find active users by role name.
     */
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.isActive = true")
    Page<User> findByRoleNamePaged(@Param("roleName") String roleName, Pageable pageable);

    /**
     * Find all active users by role name (for bulk operations).
     */
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.isActive = true")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * Find user by email verification token.
     */
    Optional<User> findByEmailVerificationToken(String token);

    /**
     * Find user by password reset token.
     */
    Optional<User> findByPasswordResetToken(String token);

    /**
     * Update last login timestamp.
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :timestamp WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") UUID userId, @Param("timestamp") Instant timestamp);

    /**
     * Search users by name or email.
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    /**
     * Count active users.
     */
    long countByIsActiveTrue();
}
