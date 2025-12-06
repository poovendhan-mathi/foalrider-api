package com.foalrider.modules.review.dto;

import com.foalrider.modules.review.entity.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin moderation actions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerateReviewRequest {

    @NotNull(message = "Status is required")
    private ReviewStatus status;

    @Size(max = 1000, message = "Admin response cannot exceed 1000 characters")
    private String adminResponse;
}
