package com.foalrider.modules.user.entity;

import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Refresh token entity for JWT refresh tokens.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_info", columnDefinition = "jsonb")
    private Map<String, String> deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "is_revoked")
    @Builder.Default
    private Boolean isRevoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Check if token is expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if token is valid (not expired and not revoked).
     */
    public boolean isValid() {
        return !isExpired() && (isRevoked == null || !isRevoked);
    }

    /**
     * Revoke this token.
     */
    public void revoke() {
        this.isRevoked = true;
        this.revokedAt = Instant.now();
    }
}
