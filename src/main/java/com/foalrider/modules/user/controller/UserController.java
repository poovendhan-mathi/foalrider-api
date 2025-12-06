package com.foalrider.modules.user.controller;

import com.foalrider.modules.user.dto.*;
import com.foalrider.modules.user.service.UserService;
import com.foalrider.shared.dto.ApiResponse;
import com.foalrider.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User profile controller for authenticated users.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse user = userService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(user, "Profile retrieved successfully"));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse user = userService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success(user, "Profile updated successfully"));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }

    // ==================== Admin Endpoints ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users (Admin only)")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(users), "Users retrieved successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search users by email or name (Admin only)")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> searchUsers(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserResponse> users = userService.searchUsers(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(users), "Search results"));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID userId) {
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully"));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> adminUpdateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        UserResponse user = userService.adminUpdateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success(user, "User updated successfully"));
    }

    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable UUID userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deactivated successfully"));
    }

    @PostMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable UUID userId) {
        userService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User activated successfully"));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }
}
