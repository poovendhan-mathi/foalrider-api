package com.foalrider.audit;

import com.foalrider.modules.auth.dto.*;
import com.foalrider.modules.auth.service.AuthServiceImpl;
import com.foalrider.modules.cart.dto.AddToCartRequest;
import com.foalrider.modules.notification.service.EmailService;
import com.foalrider.modules.order.dto.*;
import com.foalrider.modules.review.dto.*;
import com.foalrider.modules.user.entity.*;
import com.foalrider.modules.user.repository.*;
import com.foalrider.security.CustomUserDetails;
import com.foalrider.security.jwt.JwtTokenProvider;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Local secure-behavior probes; deliberately excluded from normal *Test discovery. */
class IdentityAndValidationAuditProbe {
    private UserRepository users;
    private RoleRepository roles;
    private RefreshTokenRepository refresh;
    private EmailService email;
    private AuthServiceImpl auth;
    private User user;
    private BCryptPasswordEncoder encoder;

    @BeforeEach void setup() {
        users = mock(UserRepository.class);
        roles = mock(RoleRepository.class);
        refresh = mock(RefreshTokenRepository.class);
        email = mock(EmailService.class);
        encoder = new BCryptPasswordEncoder(4);
        JwtTokenProvider tokens = mock(JwtTokenProvider.class);
        when(tokens.generateAccessToken(any(CustomUserDetails.class))).thenReturn("local-access-token");
        when(tokens.generateRefreshToken()).thenReturn("local-refresh-token");
        when(tokens.getRefreshTokenExpiration()).thenReturn(60000L);
        auth = new AuthServiceImpl(users, roles, refresh, encoder, mock(AuthenticationManager.class), tokens, email);
        Role role = Role.builder().name("ROLE_CUSTOMER").build();
        user = User.builder().role(role).email("audit@example.invalid").firstName("Audit").lastName("User")
                .passwordHash(encoder.encode("Old-password-1!")).build();
        user.setId(UUID.randomUUID());
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(roles.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(role));
        when(users.save(any())).thenAnswer(call -> {
            User saved = call.getArgument(0);
            if (saved.getId() == null) saved.setId(UUID.randomUUID());
            return saved;
        });
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void passwordChangeMustRevokeRefreshSessions() {
        auth.changePassword(com.foalrider.modules.user.dto.ChangePasswordRequest.builder()
                .currentPassword("Old-password-1!").newPassword("New-password-2!").confirmPassword("New-password-2!").build());
        verify(refresh).revokeAllByUserId(eq(user.getId()), any(Instant.class));
    }

    @Test void passwordResetRevokesRefreshSessionsAndConsumesResetToken() {
        user.setPasswordResetToken("stored-hash");
        user.setPasswordResetTokenExpiresAt(Instant.now().plusSeconds(60));
        when(users.findByPasswordResetToken(anyString())).thenReturn(Optional.of(user));
        auth.resetPassword(ResetPasswordRequest.builder().token("local-reset-token")
                .newPassword("New-password-2!").confirmPassword("New-password-2!").build());
        verify(refresh).revokeAllByUserId(eq(user.getId()), any(Instant.class));
        assertThat(user.getPasswordResetToken()).isNull();
        assertThat(user.getPasswordResetTokenExpiresAt()).isNull();
        assertThat(encoder.matches("New-password-2!", user.getPasswordHash())).isTrue();
    }

    @Test void expiredPasswordResetTokenIsRejected() {
        user.setPasswordResetTokenExpiresAt(Instant.now().minusSeconds(60));
        when(users.findByPasswordResetToken(anyString())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> auth.resetPassword(ResetPasswordRequest.builder().token("expired-local-token")
                .newPassword("New-password-2!").confirmPassword("New-password-2!").build())).hasMessageContaining("expired");
        verify(users, never()).save(any());
    }

    @Test void registrationMustSendInitialVerification() {
        auth.register(RegisterRequest.builder().email("new@example.invalid").password("New-password-2!")
                .firstName("Audit").lastName("User").build(), new MockHttpServletRequest());
        verify(email).sendVerificationEmail(eq("new@example.invalid"), eq("Audit"), anyString());
    }

    @Test void bcryptMustNotAcceptDifferentSuffixBeyond72Bytes() {
        String prefix = "a".repeat(72);
        String hash = encoder.encode(prefix + "-original");
        assertThat(encoder.matches(prefix + "-different", hash)).as("SEC-010 / CVE-2025-22228").isFalse();
    }

    @Test void oversizedCartQuantityMustFailValidation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(AddToCartRequest.builder().productId(UUID.randomUUID())
                    .quantity(Integer.MAX_VALUE).build())).as("SEC-012: business quantity cap").isNotEmpty();
        }
    }

    @Test void zeroCartQuantityIsRejected() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(AddToCartRequest.builder().productId(UUID.randomUUID())
                    .quantity(0).build())).isNotEmpty();
        }
    }

    @Test void reviewImageValidationMustCascade() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(CreateReviewRequest.builder().rating(5)
                    .images(List.of(ReviewImageRequest.builder().url("").build())).build()))
                    .as("SEC-012: validate nested image DTOs").isNotEmpty();
        }
    }

    @Test void billingAddressValidationMustCascade() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            ShippingAddressRequest shipping = ShippingAddressRequest.builder().name("Audit User").phone("123456789")
                    .addressLine1("1 Test Road").city("Test").postalCode("12345").country("US").build();
            CreateOrderRequest request = CreateOrderRequest.builder().shippingAddress(shipping)
                    .billingAddress(BillingAddressRequest.builder().name("x".repeat(101)).build()).build();
            assertThat(factory.getValidator().validate(request)).as("SEC-012: validate billing DTO").isNotEmpty();
        }
    }
}
