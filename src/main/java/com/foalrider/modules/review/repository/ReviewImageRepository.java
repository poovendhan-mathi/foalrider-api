package com.foalrider.modules.review.repository;

import com.foalrider.modules.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ReviewImage entity.
 */
@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, UUID> {

    /**
     * Find images by review ID.
     */
    List<ReviewImage> findByReviewIdOrderBySortOrderAsc(UUID reviewId);

    /**
     * Delete all images for a review.
     */
    void deleteByReviewId(UUID reviewId);

    /**
     * Count images for a review.
     */
    long countByReviewId(UUID reviewId);
}
