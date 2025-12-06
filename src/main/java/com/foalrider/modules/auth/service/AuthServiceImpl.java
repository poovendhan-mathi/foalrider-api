package com.foalrider.modules.auth.service;

import com.foalrider.modules.auth.dto.AuthResponse;
import com.foalrider.modules.auth.dto.LoginRequest;
import com.foalrider.modules.auth.dto.RefreshTokenRequest;
import com.foalrider.modules.auth.dto.RegisterRequest;
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
import java.time.Instant;
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
