package com.foalrider.modules.auth.service;

import com.foalrider.modules.auth.dto.*;
import com.foalrider.modules.notification.service.EmailService;
import com.foalrider.modules.user.dto.ChangePasswordRequest;
import com.foalrider.modules.user.entity.RefreshToken;
import com.foalrider.modules.user.entity.Role;
import com.foalrider.modules.user.entity.User;
import com.foalrider.modules.user.repository.RefreshTokenRepository;
import com.foalrider.modules.user.repository.RoleRepository;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.security.CustomUserDetails;
import com.foalrider.security.SecurityUtils;
import com.foalrider.security.jwt.JwtTokenProvider;
import com.foalrider.shared.constants.AppConstants;
import com.foalrider.shared.exception.BadRequestException;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.foalrider.shared.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of AuthService for authentication operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    
    private static final int PASSWORD_RESET_TOKEN_EXPIRY_HOURS = 1;
    private static final int EMAIL_VERIFICATION_TOKEN_EXPIRY_HOURS = 24;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new BadRequestException("Email is already registered");
        }

        // Get customer role
        Role customerRole = roleRepository.findByName(AppConstants.ROLE_CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));

        // Create user
        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(customerRole)
                .isActive(true)
                .isEmailVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // Generate tokens
        return generateAuthResponse(user, httpRequest);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(),
                        request.getPassword()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Get user
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Check if account is active
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        // Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());

        // Generate tokens
        return generateAuthResponse(user, httpRequest);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String tokenHash = hashToken(request.getRefreshToken());

        // Find refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Validate token
        if (!refreshToken.isValid()) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();

        // Check if user is active
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        // Revoke old token
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        log.info("Token refreshed for user: {}", user.getEmail());

        // Generate new tokens
        return generateAuthResponse(user, httpRequest);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return;
        }

        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                    log.info("User logged out");
                });
    }

    @Override
    @Transactional
    public void logoutAll() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        log.info("User logged out from all devices: {}", userId);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase();
        
        userRepository.findByEmail(email).ifPresent(user -> {
            // Generate reset token
            String resetToken = generateSecureToken();
            
            // Save token to user
            user.setPasswordResetToken(hashToken(resetToken));
            user.setPasswordResetTokenExpiresAt(Instant.now().plus(PASSWORD_RESET_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS));
            userRepository.save(user);
            
            // Send email
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetToken);
            log.info("Password reset email sent to: {}", email);
        });
        
        // Always return success to prevent email enumeration
        log.info("Forgot password requested for: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        
        String tokenHash = hashToken(request.getToken());
        
        User user = userRepository.findByPasswordResetToken(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        
        // Check if token is expired
        if (user.getPasswordResetTokenExpiresAt() == null || 
            user.getPasswordResetTokenExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Reset token has expired");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);
        
        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());
        
        log.info("Password reset successful for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        
        UUID userId = SecurityUtils.requireCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        
        // Check if new password is same as current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        log.info("Password changed for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String tokenHash = hashToken(request.getToken());
        
        User user = userRepository.findByEmailVerificationToken(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));
        
        // Check if token is expired
        if (user.getEmailVerificationTokenExpiresAt() == null || 
            user.getEmailVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Verification token has expired");
        }
        
        // Mark email as verified
        user.setIsEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiresAt(null);
        userRepository.save(user);
        
        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
        
        log.info("Email verified for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resendVerificationEmail() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getIsEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }
        
        // Generate new verification token
        String verificationToken = generateSecureToken();
        user.setEmailVerificationToken(hashToken(verificationToken));
        user.setEmailVerificationTokenExpiresAt(Instant.now().plus(EMAIL_VERIFICATION_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS));
        userRepository.save(user);
        
        // Send email
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verificationToken);
        
        log.info("Verification email resent to: {}", user.getEmail());
    }

    /**
     * Generate a secure random token.
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate auth response with access and refresh tokens.
     */
    private AuthResponse generateAuthResponse(User user, HttpServletRequest request) {
        // Create user details
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Generate access token
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);

        // Generate refresh token
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        // Save refresh token
        saveRefreshToken(user, refreshToken, request);

        // Build user info
        AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().getName())
                .avatarUrl(user.getAvatarUrl())
                .build();

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationInSeconds(),
                userInfo
        );
    }

    /**
     * Save refresh token to database.
     */
    private void saveRefreshToken(User user, String token, HttpServletRequest request) {
        String tokenHash = hashToken(token);

        Map<String, String> deviceInfo = new HashMap<>();
        deviceInfo.put("userAgent", request.getHeader("User-Agent"));
        deviceInfo.put("origin", request.getHeader("Origin"));

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .deviceInfo(deviceInfo)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpiration()))
                .isRevoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Hash token using SHA-256.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    /**
     * Get client IP address from request.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
