package com.foalrider.modules.auth.service;

import com.foalrider.modules.auth.dto.AuthResponse;
import com.foalrider.modules.auth.dto.LoginRequest;
import com.foalrider.modules.auth.dto.RefreshTokenRequest;
import com.foalrider.modules.auth.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Register a new user.
     */
    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    /**
     * Authenticate user and return tokens.
     */
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    /**
     * Refresh access token using refresh token.
     */
    AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest);

    /**
     * Logout user and revoke refresh token.
     */
    void logout(String refreshToken);

    /**
     * Logout from all devices (revoke all refresh tokens).
     */
    void logoutAll();
}
