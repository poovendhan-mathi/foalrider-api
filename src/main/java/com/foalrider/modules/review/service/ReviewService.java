package com.foalrider.modules.review.service;

import com.foalrider.modules.review.dto.*;
import com.foalrider.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for review operations.
 */
public interface ReviewService {

    /**
     * Create a new review.
     */
    ReviewResponse createReview(CreateReviewRequest request, UUID userId);

    /**
     * Update an existing review.
     */
    ReviewResponse updateReview(UUID reviewId, UpdateReviewRequest request, UUID userId);

    /**
     * Delete a review.
     */
    void deleteReview(UUID reviewId, UUID userId);

    /**
     * Get review by ID.
     */
    ReviewResponse getReview(UUID reviewId, UUID currentUserId);

    /**
     * Get reviews for a product (approved only for public).
     */
    PagedResponse<ReviewResponse> getProductReviews(UUID productId, UUID currentUserId, Pageable pageable);

    /**
     * Get verified purchase reviews for a product.
     */
    PagedResponse<ReviewResponse> getVerifiedReviews(UUID productId, UUID currentUserId, Pageable pageable);

    /**
     * Get reviews by user.
     */
    PagedResponse<ReviewResponse> getUserReviews(UUID userId, Pageable pageable);

    /**
     * Get current user's reviews.
     */
    PagedResponse<ReviewResponse> getMyReviews(UUID userId, Pageable pageable);

    /**
     * Vote on a review (helpful/not helpful).
     */
    ReviewResponse voteReview(UUID reviewId, ReviewVoteRequest request, UUID userId);

    /**
     * Remove vote from a review.
     */
    void removeVote(UUID reviewId, UUID userId);

    /**
     * Get review statistics for a product.
     */
    ReviewStatsResponse getReviewStats(UUID productId);

    /**
     * Check if user can review a product.
     */
    boolean canUserReviewProduct(UUID userId, UUID productId);

    // Admin methods

    /**
     * Get reviews pending moderation.
     */
    PagedResponse<ReviewResponse> getPendingReviews(Pageable pageable);

    /**
     * Get all reviews (admin).
     */
    PagedResponse<ReviewResponse> getAllReviews(Pageable pageable);

    /**
     * Moderate a review (approve/reject).
     */
    ReviewResponse moderateReview(UUID reviewId, ModerateReviewRequest request);

    /**
     * Add admin response to a review.
     */
    ReviewResponse addAdminResponse(UUID reviewId, String response);
}
