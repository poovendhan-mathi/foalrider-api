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
import com.foalrider.security.jwt.JwtTokenProvider;
import com.foalrider.shared.constants.AppConstants;
import com.foalrider.shared.exception.BadRequestException;
import com.foalrider.shared.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 * Tests authentication operations including registration, login, logout, and token refresh.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService Tests")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private Role customerRole;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Setup customer role
        customerRole = Role.builder()
                .name(AppConstants.ROLE_CUSTOMER)
                .description("Customer role")
                .build();
        setEntityId(customerRole, UUID.randomUUID());

        // Setup test user
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .role(customerRole)
                .isActive(true)
                .isEmailVerified(false)
                .build();
        setEntityId(testUser, UUID.randomUUID());

        // Setup register request
        registerRequest = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("Password123!")
                .firstName("Jane")
                .lastName("Smith")
                .phone("+0987654321")
                .build();

        // Setup login request
        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();

        // Mock HTTP request
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("Test-Agent");
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register new user successfully")
        void register_WithValidRequest_ShouldSucceed() {
            // Arrange
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(roleRepository.findByName(AppConstants.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AuthResponse response = authService.register(registerRequest, httpRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isNotNull();
            
            verify(userRepository).existsByEmail("newuser@example.com");
            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("Password123!");
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void register_WithExistingEmail_ShouldThrowException() {
            // Arrange
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.register(registerRequest, httpRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email is already registered");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void register_ShouldNormalizeEmail() {
            // Arrange
            registerRequest.setEmail("NewUser@Example.COM");
            when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
            when(roleRepository.findByName(AppConstants.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            authService.register(registerRequest, httpRequest);

            // Assert
            verify(userRepository).existsByEmail("newuser@example.com");
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void login_WithValidCredentials_ShouldSucceed() {
            // Arrange
            CustomUserDetails userDetails = new CustomUserDetails(testUser);
            Authentication authentication = mock(Authentication.class);
            
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AuthResponse response = authService.login(loginRequest, httpRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void login_WithInvalidCredentials_ShouldThrowException() {
            // Arrange
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest, httpRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Should throw exception for disabled account")
        void login_WithDisabledAccount_ShouldThrowException() {
            // Arrange
            testUser.setIsActive(false);
            CustomUserDetails userDetails = new CustomUserDetails(testUser);
            Authentication authentication = mock(Authentication.class);
            
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest, httpRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Account is disabled");
        }

        @Test
        @DisplayName("Should update last login timestamp")
        void login_ShouldUpdateLastLoginTimestamp() {
            // Arrange
            CustomUserDetails userDetails = new CustomUserDetails(testUser);
            Authentication authentication = mock(Authentication.class);
            
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            authService.login(loginRequest, httpRequest);

            // Assert
            verify(userRepository).save(argThat(user -> user.getLastLoginAt() != null));
        }
    }

    @Nested
    @DisplayName("Token Refresh Tests")
    class TokenRefreshTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void refreshToken_WithValidToken_ShouldSucceed() {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
            RefreshToken refreshToken = RefreshToken.builder()
                    .tokenHash("hashed-token")
                    .user(testUser)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .isRevoked(false)
                    .build();
            setEntityId(refreshToken, UUID.randomUUID());

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtTokenProvider.generateAccessToken(any(CustomUserDetails.class))).thenReturn("new-access-token");
            when(jwtTokenProvider.generateRefreshToken()).thenReturn("new-refresh-token");

            // Act
            AuthResponse response = authService.refreshToken(request, httpRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        }

        @Test
        @DisplayName("Should throw exception for expired refresh token")
        void refreshToken_WithExpiredToken_ShouldThrowException() {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest("expired-refresh-token");
            RefreshToken refreshToken = RefreshToken.builder()
                    .tokenHash("hashed-token")
                    .user(testUser)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS)) // Expired
                    .isRevoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(request, httpRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("expired or revoked");
        }

        @Test
        @DisplayName("Should throw exception for revoked refresh token")
        void refreshToken_WithRevokedToken_ShouldThrowException() {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest("revoked-refresh-token");
            RefreshToken refreshToken = RefreshToken.builder()
                    .tokenHash("hashed-token")
                    .user(testUser)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .isRevoked(true) // Revoked
                    .build();
            setEntityId(refreshToken, UUID.randomUUID());

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(request, httpRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("expired or revoked");
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void refreshToken_WithInvalidToken_ShouldThrowException() {
            // Arrange
            RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(request, httpRequest))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid refresh token");
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully and revoke token")
        void logout_WithValidToken_ShouldRevokeToken() {
            // Arrange
            String refreshToken = "valid-refresh-token";
            RefreshToken token = RefreshToken.builder()
                    .tokenHash("hashed-token")
                    .user(testUser)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .isRevoked(false)
                    .build();
            setEntityId(token, UUID.randomUUID());

            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            authService.logout(refreshToken);

            // Assert
            verify(refreshTokenRepository).save(argThat(t -> t.getIsRevoked()));
        }

        @Test
        @DisplayName("Should handle logout with invalid token gracefully")
        void logout_WithInvalidToken_ShouldNotThrow() {
            // Arrange
            when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            // Act & Assert - Should not throw
            assertThatCode(() -> authService.logout("invalid-token"))
                    .doesNotThrowAnyException();
        }
    }

    /**
     * Helper method to set ID on entities that extend BaseEntity.
     * Used in tests because ID is not part of the Lombok builder (it's in the parent class).
     */
    private void setEntityId(Object entity, UUID id) {
        try {
            java.lang.reflect.Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set entity ID", e);
        }
    }
}
