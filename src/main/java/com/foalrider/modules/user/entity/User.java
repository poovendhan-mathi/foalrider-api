package com.foalrider.modules.user.entity;

import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * User entity representing application users.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "is_email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "email_verification_token_expires_at")
    private Instant emailVerificationTokenExpiresAt;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expires_at")
    private Instant passwordResetTokenExpiresAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // Regional & Locale preferences
    @Column(name = "region_code", length = 2)
    @Builder.Default
    private String regionCode = "US"; // ISO 3166-1 alpha-2: US, SG, IN, GB

    @Column(name = "preferred_currency", length = 3)
    private String preferredCurrency; // ISO 4217: USD, SGD, INR, GBP (null = use region default)

    @Column(name = "locale", length = 10)
    @Builder.Default
    private String locale = "en-US"; // en-US, en-SG, en-IN, en-GB

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    /**
     * Get full name of the user.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Check if account is enabled and active.
     */
    public boolean isEnabled() {
        return isActive != null && isActive;
    }

    /**
     * Check if account is locked (not active).
     */
    public boolean isAccountNonLocked() {
        return isActive != null && isActive;
    }
}
