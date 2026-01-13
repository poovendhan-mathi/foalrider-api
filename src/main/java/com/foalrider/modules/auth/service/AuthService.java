package com.foalrider.modules.auth.service;

import com.foalrider.modules.auth.dto.*;
import com.foalrider.modules.user.dto.ChangePasswordRequest;
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

    /**
     * Initiate forgot password flow - sends reset email.
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Reset password using token from email.
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * Change password for authenticated user.
     */
    void changePassword(ChangePasswordRequest request);

    /**
     * Verify user email using token.
     */
    void verifyEmail(VerifyEmailRequest request);

    /**
     * Resend email verification link.
     */
    void resendVerificationEmail();
}
