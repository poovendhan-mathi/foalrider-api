package com.foalrider.modules.review.dto;

import com.foalrider.modules.review.entity.FitFeedback;
import com.foalrider.modules.review.entity.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for reviews.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private String productSlug;
    private UUID userId;
    private String userName;
    private String userAvatarUrl;
    private Integer rating;
    private String title;
    private String content;
    private List<String> pros;
    private List<String> cons;
    private FitFeedback fitFeedback;
    private Boolean isVerifiedPurchase;
    private ReviewStatus status;
    private Integer helpfulCount;
    private Integer notHelpfulCount;
    private String adminResponse;
    private List<ReviewImageResponse> images;
    private Boolean userVotedHelpful; // Current user's vote (null if not voted)
    private Instant createdAt;
    private Instant updatedAt;
}
