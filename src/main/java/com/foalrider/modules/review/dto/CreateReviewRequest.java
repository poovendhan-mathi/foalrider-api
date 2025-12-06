package com.foalrider.modules.review.dto;

import com.foalrider.modules.review.entity.FitFeedback;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    // Note: productId is set from path variable in controller, not from request body
    private UUID productId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Content cannot exceed 5000 characters")
    private String content;

    private List<String> pros;

    private List<String> cons;

    private FitFeedback fitFeedback;

    private List<ReviewImageRequest> images;
}
