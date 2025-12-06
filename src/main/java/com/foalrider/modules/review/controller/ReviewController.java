package com.foalrider.modules.review.controller;

import com.foalrider.modules.review.dto.*;
import com.foalrider.modules.review.service.ReviewService;
import com.foalrider.security.SecurityUtils;
import com.foalrider.shared.dto.ApiResponse;
import com.foalrider.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for review operations.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product review and rating management")
public class ReviewController {

    private final ReviewService reviewService;

    // ==================== PUBLIC ENDPOINTS ====================

    @GetMapping("/products/{productId}/reviews")
    @Operation(summary = "Get product reviews", description = "Get approved reviews for a product")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        UUID currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        PagedResponse<ReviewResponse> reviews = reviewService.getProductReviews(productId, currentUserId, pageable);

        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/products/{productId}/reviews/verified")
    @Operation(summary = "Get verified purchase reviews", description = "Get reviews from verified purchasers")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getVerifiedReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        UUID currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        PagedResponse<ReviewResponse> reviews = reviewService.getVerifiedReviews(productId, currentUserId, pageable);

        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/products/{productId}/reviews/stats")
    @Operation(summary = "Get review statistics", description = "Get rating statistics for a product")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> getReviewStats(@PathVariable UUID productId) {
        ReviewStatsResponse stats = reviewService.getReviewStats(productId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/reviews/{reviewId}")
    @Operation(summary = "Get review by ID", description = "Get a specific review")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReview(@PathVariable UUID reviewId) {
        UUID currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        ReviewResponse review = reviewService.getReview(reviewId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(review));
    }

    // ==================== AUTHENTICATED USER ENDPOINTS ====================

    @PostMapping("/products/{productId}/reviews")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create review", description = "Create a new product review")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateReviewRequest request) {

        request.setProductId(productId);
        UUID userId = SecurityUtils.requireCurrentUserId();
        ReviewResponse review = reviewService.createReview(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(review, "Review created successfully"));
    }

    @PutMapping("/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update review", description = "Update your own review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {

        UUID userId = SecurityUtils.requireCurrentUserId();
        ReviewResponse review = reviewService.updateReview(reviewId, request, userId);

        return ResponseEntity.ok(ApiResponse.success(review, "Review updated successfully"));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete review", description = "Delete your own review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable UUID reviewId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully"));
    }

    @PostMapping("/reviews/{reviewId}/vote")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Vote on review", description = "Mark a review as helpful or not helpful")
    public ResponseEntity<ApiResponse<ReviewResponse>> voteReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewVoteRequest request) {

        UUID userId = SecurityUtils.requireCurrentUserId();
        ReviewResponse review = reviewService.voteReview(reviewId, request, userId);

        return ResponseEntity.ok(ApiResponse.success(review, "Vote recorded"));
    }

    @DeleteMapping("/reviews/{reviewId}/vote")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove vote", description = "Remove your vote from a review")
    public ResponseEntity<ApiResponse<Void>> removeVote(@PathVariable UUID reviewId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        reviewService.removeVote(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success("Vote removed"));
    }

    @GetMapping("/users/me/reviews")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my reviews", description = "Get current user's reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        UUID userId = SecurityUtils.requireCurrentUserId();
        PagedResponse<ReviewResponse> reviews = reviewService.getMyReviews(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/products/{productId}/can-review")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check if can review", description = "Check if user can review a product")
    public ResponseEntity<ApiResponse<Boolean>> canReviewProduct(@PathVariable UUID productId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        boolean canReview = reviewService.canUserReviewProduct(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(canReview));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all reviews (Admin)", description = "Get all reviews with any status")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PagedResponse<ReviewResponse> reviews = reviewService.getAllReviews(pageable);

        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/admin/reviews/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending reviews (Admin)", description = "Get reviews awaiting moderation")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        PagedResponse<ReviewResponse> reviews = reviewService.getPendingReviews(pageable);

        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @PutMapping("/admin/reviews/{reviewId}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Moderate review (Admin)", description = "Approve or reject a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ModerateReviewRequest request) {

        ReviewResponse review = reviewService.moderateReview(reviewId, request);
        return ResponseEntity.ok(ApiResponse.success(review, "Review moderated successfully"));
    }

    @PostMapping("/admin/reviews/{reviewId}/response")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add admin response (Admin)", description = "Add admin response to a review")
    public ResponseEntity<ApiResponse<ReviewResponse>> addAdminResponse(
            @PathVariable UUID reviewId,
            @RequestBody String response) {

        ReviewResponse review = reviewService.addAdminResponse(reviewId, response);
        return ResponseEntity.ok(ApiResponse.success(review, "Admin response added"));
    }
}
