package com.foalrider.modules.auth.controller;

import com.foalrider.modules.auth.dto.AuthResponse;
import com.foalrider.modules.auth.dto.LoginRequest;
import com.foalrider.modules.auth.dto.RefreshTokenRequest;
import com.foalrider.modules.auth.dto.RegisterRequest;
import com.foalrider.modules.auth.service.AuthService;
import com.foalrider.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Create a new user account and return authentication tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Registration successful"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user and return access and refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        AuthResponse response = authService.refreshToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revoke the provided refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request) {
        
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices", description = "Revoke all refresh tokens for the current user")
    public ResponseEntity<ApiResponse<Void>> logoutAll() {
        authService.logoutAll();
        return ResponseEntity.ok(ApiResponse.success("Logged out from all devices"));
    }
}
