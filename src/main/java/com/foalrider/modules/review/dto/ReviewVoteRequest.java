package com.foalrider.modules.review.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for voting on a review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVoteRequest {

    @NotNull(message = "Vote value is required")
    private Boolean isHelpful; // true = helpful, false = not helpful
}
