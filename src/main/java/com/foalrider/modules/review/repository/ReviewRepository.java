package com.foalrider.modules.review.repository;

import com.foalrider.modules.review.entity.Review;
import com.foalrider.modules.review.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Review entity.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /**
     * Find reviews by product ID with approved status.
     */
    Page<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    /**
     * Find reviews by product ID (all statuses).
     */
    Page<Review> findByProductId(UUID productId, Pageable pageable);

    /**
     * Find reviews by user ID.
     */
    Page<Review> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find review by user and product (check if already reviewed).
     */
    Optional<Review> findByUserIdAndProductId(UUID userId, UUID productId);

    /**
     * Check if user has reviewed a product.
     */
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    /**
     * Find reviews by status (for moderation).
     */
    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    /**
     * Calculate average rating for a product.
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.status = 'APPROVED'")
    Double calculateAverageRating(@Param("productId") UUID productId);

    /**
     * Count approved reviews for a product.
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.status = 'APPROVED'")
    Long countApprovedReviews(@Param("productId") UUID productId);

    /**
     * Get rating distribution for a product.
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.status = 'APPROVED' GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistribution(@Param("productId") UUID productId);

    /**
     * Find reviews by verified purchase status.
     */
    Page<Review> findByProductIdAndStatusAndIsVerifiedPurchase(
            UUID productId, ReviewStatus status, Boolean isVerifiedPurchase, Pageable pageable);
}
