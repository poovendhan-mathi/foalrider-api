package com.foalrider.security;

import com.foalrider.modules.user.entity.User;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Security utility methods for accessing current user information.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Get current authenticated user details.
     */
    public static Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        
        if (principal instanceof CustomUserDetails) {
            return Optional.of((CustomUserDetails) principal);
        }
        
        return Optional.empty();
    }

    /**
     * Get current user ID.
     */
    public static Optional<UUID> getCurrentUserId() {
        return getCurrentUserDetails().map(CustomUserDetails::getId);
    }

    /**
     * Get current user email.
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentUserDetails().map(CustomUserDetails::getEmail);
    }

    /**
     * Get current user role.
     */
    public static Optional<String> getCurrentUserRole() {
        return getCurrentUserDetails().map(CustomUserDetails::getRoleName);
    }

    /**
     * Check if current user has a specific role.
     */
    public static boolean hasRole(String role) {
        return getCurrentUserRole()
                .map(r -> r.equals(role))
                .orElse(false);
    }

    /**
     * Check if current user is authenticated.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               authentication.getPrincipal() instanceof CustomUserDetails;
    }

    /**
     * Get current authenticated user entity.
     * @throws UnauthorizedException if not authenticated
     */
    public User getCurrentUser() {
        UUID userId = getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));
        
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    /**
     * Get current user ID or throw exception.
     */
    public static UUID requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));
    }

    /**
     * Get current user ID or null if not authenticated.
     */
    public static UUID getCurrentUserIdOrNull() {
        return getCurrentUserId().orElse(null);
    }
}
