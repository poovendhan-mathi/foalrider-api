package com.foalrider.modules.review.repository;

import com.foalrider.modules.review.entity.ReviewVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ReviewVote entity.
 */
@Repository
public interface ReviewVoteRepository extends JpaRepository<ReviewVote, UUID> {

    /**
     * Find vote by user and review.
     */
    Optional<ReviewVote> findByUserIdAndReviewId(UUID userId, UUID reviewId);

    /**
     * Check if user has voted on a review.
     */
    boolean existsByUserIdAndReviewId(UUID userId, UUID reviewId);

    /**
     * Delete vote by user and review.
     */
    void deleteByUserIdAndReviewId(UUID userId, UUID reviewId);

    /**
     * Count helpful votes for a review.
     */
    long countByReviewIdAndIsHelpful(UUID reviewId, Boolean isHelpful);
}
