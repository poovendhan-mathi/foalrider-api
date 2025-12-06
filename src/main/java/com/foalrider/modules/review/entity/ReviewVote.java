package com.foalrider.modules.review.entity;

import com.foalrider.modules.user.entity.User;
import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * ReviewVote entity - Tracks user votes on reviews (helpful/not helpful).
 */
@Entity
@Table(name = "review_votes", indexes = {
    @Index(name = "idx_review_vote_review", columnList = "review_id"),
    @Index(name = "idx_review_vote_user", columnList = "user_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_review_vote_user_review", columnNames = {"user_id", "review_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewVote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_helpful", nullable = false)
    private Boolean isHelpful; // true = helpful, false = not helpful
}
