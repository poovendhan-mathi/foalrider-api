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
import com.foalrider.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User service implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User savedUser = userRepository.save(user);
        log.info("User profile updated: {}", savedUser.getEmail());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        User user = getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = getUserEntityById(userId);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable)
                .map(userMapper::toResponse);
    }

    @Override
    public UserResponse adminUpdateUser(UUID userId, AdminUpdateUserRequest request) {
        User user = getUserEntityById(userId);

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "id", request.getRoleId()));
            user.setRole(role);
        }

        User savedUser = userRepository.save(user);
        log.info("Admin updated user: {}", savedUser.getEmail());
        return userMapper.toResponse(savedUser);
    }

    @Override
    public void deactivateUser(UUID userId) {
        User user = getUserEntityById(userId);
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User deactivated: {}", user.getEmail());
    }

    @Override
    public void activateUser(UUID userId) {
        User user = getUserEntityById(userId);
        user.setIsActive(true);
        userRepository.save(user);
        log.info("User activated: {}", user.getEmail());
    }

    @Override
    public void deleteUser(UUID userId) {
        User user = getUserEntityById(userId);
        userRepository.delete(user);
        log.info("User deleted: {}", user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private User getCurrentUser() {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
