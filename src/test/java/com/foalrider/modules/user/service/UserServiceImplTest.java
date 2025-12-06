package com.foalrider.modules.user.service;

import com.foalrider.modules.user.dto.*;
import com.foalrider.modules.user.entity.Role;
import com.foalrider.modules.user.entity.User;
import com.foalrider.modules.user.mapper.UserMapper;
import com.foalrider.modules.user.repository.RoleRepository;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.security.SecurityUtils;
import com.foalrider.shared.exception.BadRequestException;
import com.foalrider.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserServiceImpl.
 * Tests user profile operations, password management, and admin user management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserResponse userResponse;
    private UUID userId;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        customerRole = Role.builder()
                .name("CUSTOMER")
                .build();
        customerRole.setId(UUID.randomUUID());

        testUser = User.builder()
                .email("user@test.com")
                .passwordHash("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .role(customerRole)
                .isActive(true)
                .isEmailVerified(true)
                .build();
        testUser.setId(userId);

        userResponse = UserResponse.builder()
                .id(userId)
                .email("user@test.com")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .role("CUSTOMER")
                .isActive(true)
                .isEmailVerified(true)
                .build();
    }

    @Nested
    @DisplayName("Get Current User Profile Tests")
    class GetCurrentUserProfileTests {

        @Test
        @DisplayName("Should get current user profile successfully")
        void getCurrentUserProfile_ShouldReturnProfile() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(userId));
                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(userMapper.toResponse(testUser)).thenReturn(userResponse);

                // Act
                UserResponse response = userService.getCurrentUserProfile();

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(userId);
                assertThat(response.getEmail()).isEqualTo("user@test.com");
                assertThat(response.getFirstName()).isEqualTo("John");
            }
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getCurrentUserProfile_WithInvalidUser_ShouldThrowException() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(userId));
                when(userRepository.findById(userId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> userService.getCurrentUserProfile())
                        .isInstanceOf(ResourceNotFoundException.class);
            }
        }
    }

    @Nested
    @DisplayName("Update Profile Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update profile successfully")
        void updateProfile_WithValidRequest_ShouldSucceed() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(userId));
                
                UpdateProfileRequest request = UpdateProfileRequest.builder()
                        .firstName("Jane")
                        .lastName("Smith")
                        .phone("+0987654321")
                        .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(userRepository.save(any(User.class))).thenReturn(testUser);
                when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

                // Act
                UserResponse response = userService.updateProfile(request);

                // Assert
                assertThat(response).isNotNull();
                verify(userRepository).save(any(User.class));
            }
        }

        @Test
        @DisplayName("Should update only provided fields")
        void updateProfile_WithPartialUpdate_ShouldUpdateOnlyProvidedFields() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(userId));
                
                UpdateProfileRequest request = UpdateProfileRequest.builder()
                        .firstName("Jane") // Only updating first name
                        .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(userRepository.save(any(User.class))).thenReturn(testUser);
                when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

                // Act
                userService.updateProfile(request);

                // Assert
                verify(userRepository).save(argThat(user -> 
                        user.getFirstName().equals("Jane") && 
                        user.getLastName().equals("Doe"))); // Last name unchanged
            }
        }
    }

    @Nested
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void changePassword_WithValidRequest_ShouldSucceed() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(userId));
                
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                        .currentPassword("OldPassword123!")
                        .newPassword("NewPassword123!")
                        .confirmPassword("NewPassword123!")
                        .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(passwordEncoder.matches("OldPassword123!", "encodedPassword")).thenReturn(true);
                when(passwordEncoder.matches("NewPassword123!", "encodedPassword")).thenReturn(false);
                when(passwordEncoder.encode("NewPassword123!")).thenReturn("newEncodedPassword");
                when(userRepository.save(any(User.class))).thenReturn(testUser);

                // Act & Assert
                assertThatCode(() -> userService.changePassword(request))
                        .doesNotThrowAnyException();
                
                verify(userRepository).save(any(User.class));
            }
        }

        @Test
        @DisplayName("Should throw exception when passwords don't match")
        void changePassword_WithMismatchedPasswords_ShouldThrowException() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(userId));
                
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                        .currentPassword("OldPassword123!")
                        .newPassword("NewPassword123!")
                        .confirmPassword("DifferentPassword123!") // Different
                        .build();

                // Act & Assert
                assertThatThrownBy(() -> userService.changePassword(request))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("do not match");
            }
        }

        @Test
        @DisplayName("Should throw exception when current password is incorrect")
        void changePassword_WithWrongCurrentPassword_ShouldThrowException() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(userId));
                
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                        .currentPassword("WrongPassword!")
                        .newPassword("NewPassword123!")
                        .confirmPassword("NewPassword123!")
                        .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(passwordEncoder.matches("WrongPassword!", "encodedPassword")).thenReturn(false);

                // Act & Assert
                assertThatThrownBy(() -> userService.changePassword(request))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("Current password is incorrect");
            }
        }

        @Test
        @DisplayName("Should throw exception when new password is same as current")
        void changePassword_WithSamePassword_ShouldThrowException() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(userId));
                
                ChangePasswordRequest request = ChangePasswordRequest.builder()
                        .currentPassword("CurrentPassword123!")
                        .newPassword("CurrentPassword123!")
                        .confirmPassword("CurrentPassword123!")
                        .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(passwordEncoder.matches("CurrentPassword123!", "encodedPassword")).thenReturn(true);

                // Act & Assert
                assertThatThrownBy(() -> userService.changePassword(request))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("must be different");
            }
        }
    }

    @Nested
    @DisplayName("Get User By ID Tests (Admin)")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should get user by ID successfully")
        void getUserById_WithValidId_ShouldReturnUser() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userMapper.toResponse(testUser)).thenReturn(userResponse);

            // Act
            UserResponse response = userService.getUserById(userId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void getUserById_WithInvalidId_ShouldThrowException() {
            // Arrange
            UUID invalidId = UUID.randomUUID();
            when(userRepository.findById(invalidId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserById(invalidId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get All Users Tests (Admin)")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should get all users with pagination")
        void getAllUsers_ShouldReturnPagedUsers() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(Arrays.asList(testUser), pageable, 1);
            
            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

            // Act
            Page<UserResponse> response = userService.getAllUsers(pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return empty page when no users")
        void getAllUsers_WithNoUsers_ShouldReturnEmptyPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            
            when(userRepository.findAll(pageable)).thenReturn(emptyPage);

            // Act
            Page<UserResponse> response = userService.getAllUsers(pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("Update User Tests (Admin)")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user status successfully")
        void updateUser_ShouldUpdateUserStatus() {
            // Arrange
            AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                    .isActive(false)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

            // Act
            UserResponse response = userService.adminUpdateUser(userId, request);

            // Assert
            assertThat(response).isNotNull();
            verify(userRepository).save(argThat(user -> !user.getIsActive()));
        }

        @Test
        @DisplayName("Should update user role successfully")
        void updateUser_ShouldUpdateUserRole() {
            // Arrange
            Role adminRole = Role.builder()
                    .name("ADMIN")
                    .build();
            adminRole.setId(UUID.randomUUID());

            AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                    .roleId(adminRole.getId())
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(roleRepository.findById(adminRole.getId())).thenReturn(Optional.of(adminRole));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

            // Act
            UserResponse response = userService.adminUpdateUser(userId, request);

            // Assert
            assertThat(response).isNotNull();
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Delete User Tests (Admin)")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user successfully")
        void deleteUser_WithValidId_ShouldSucceed() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            doNothing().when(userRepository).delete(any(User.class));

            // Act
            userService.deleteUser(userId);

            // Assert
            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("Should throw exception when user not found for deletion")
        void deleteUser_WithInvalidId_ShouldThrowException() {
            // Arrange
            UUID invalidId = UUID.randomUUID();
            when(userRepository.findById(invalidId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.deleteUser(invalidId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
