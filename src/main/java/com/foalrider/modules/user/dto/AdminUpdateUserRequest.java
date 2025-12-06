package com.foalrider.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Admin update user request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {

    private String firstName;
    private String lastName;
    private String phone;
    private Boolean isActive;
    private UUID roleId;
}
