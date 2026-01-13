package com.foalrider.modules.notification.service;

/**
 * Service interface for sending emails.
 */
public interface EmailService {

    /**
     * Send a simple text email.
     */
    void sendSimpleEmail(String to, String subject, String text);

    /**
     * Send an HTML email.
     */
    void sendHtmlEmail(String to, String subject, String htmlContent);

    /**
     * Send password reset email.
     */
    void sendPasswordResetEmail(String to, String firstName, String resetToken);

    /**
     * Send email verification email.
     */
    void sendVerificationEmail(String to, String firstName, String verificationToken);

    /**
     * Send welcome email after registration.
     */
    void sendWelcomeEmail(String to, String firstName);

    /**
     * Send order confirmation email.
     */
    void sendOrderConfirmationEmail(String to, String firstName, String orderId, String orderDetails);
}
