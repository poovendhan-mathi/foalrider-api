package com.foalrider.modules.user.service;

import com.foalrider.modules.user.dto.*;
import com.foalrider.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * User service interface.
 */
public interface UserService {

    /**
     * Get current authenticated user's profile.
     */
    UserResponse getCurrentUserProfile();

    /**
     * Update current user's profile.
     */
    UserResponse updateProfile(UpdateProfileRequest request);

    /**
     * Change current user's password.
     */
    void changePassword(ChangePasswordRequest request);

    /**
     * Get user by ID (Admin only).
     */
    UserResponse getUserById(UUID userId);

    /**
     * Get all users with pagination (Admin only).
     */
    Page<UserResponse> getAllUsers(Pageable pageable);

    /**
     * Search users by email or name (Admin only).
     */
    Page<UserResponse> searchUsers(String query, Pageable pageable);

    /**
     * Update user by admin.
     */
    UserResponse adminUpdateUser(UUID userId, AdminUpdateUserRequest request);

    /**
     * Deactivate user (Admin only).
     */
    void deactivateUser(UUID userId);

    /**
     * Activate user (Admin only).
     */
    void activateUser(UUID userId);

    /**
     * Delete user (Admin only).
     */
    void deleteUser(UUID userId);

    /**
     * Get user entity by ID.
     */
    User getUserEntityById(UUID userId);
}
