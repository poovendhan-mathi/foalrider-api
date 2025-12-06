package com.foalrider.modules.user.repository;

import com.foalrider.modules.user.entity.RefreshToken;
import com.foalrider.modules.user.entity.User;
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
 * Repository for RefreshToken entity.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by token hash.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find all valid tokens for a user.
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.isRevoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByUser(@Param("user") User user, @Param("now") Instant now);

    /**
     * Revoke all tokens for a user.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.revokedAt = :now WHERE rt.user.id = :userId AND rt.isRevoked = false")
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Delete expired tokens.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Count active tokens for user.
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.isRevoked = false AND rt.expiresAt > :now")
    long countActiveTokensByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    /**
     * Check if token hash exists.
     */
    boolean existsByTokenHash(String tokenHash);
}
