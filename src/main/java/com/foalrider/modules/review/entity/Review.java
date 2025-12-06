package com.foalrider.modules.review.entity;

import com.foalrider.modules.product.entity.Product;
import com.foalrider.modules.user.entity.User;
import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Review entity - Product reviews and ratings from customers.
 */
@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_review_product", columnList = "product_id"),
    @Index(name = "idx_review_user", columnList = "user_id"),
    @Index(name = "idx_review_rating", columnList = "rating"),
    @Index(name = "idx_review_status", columnList = "status"),
    @Index(name = "idx_review_verified", columnList = "is_verified_purchase")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_review_user_product", columnNames = {"user_id", "product_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rating", nullable = false)
    private Integer rating; // 1-5 stars

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pros", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> pros = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cons", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> cons = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "fit_feedback", length = 20)
    private FitFeedback fitFeedback;

    @Column(name = "is_verified_purchase")
    @Builder.Default
    private Boolean isVerifiedPurchase = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "helpful_count")
    @Builder.Default
    private Integer helpfulCount = 0;

    @Column(name = "not_helpful_count")
    @Builder.Default
    private Integer notHelpfulCount = 0;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReviewVote> votes = new ArrayList<>();

    /**
     * Add an image to the review.
     */
    public void addImage(ReviewImage image) {
        images.add(image);
        image.setReview(this);
    }

    /**
     * Remove an image from the review.
     */
    public void removeImage(ReviewImage image) {
        images.remove(image);
        image.setReview(null);
    }

    /**
     * Increment helpful count.
     */
    public void incrementHelpfulCount() {
        this.helpfulCount = (this.helpfulCount == null ? 0 : this.helpfulCount) + 1;
    }

    /**
     * Decrement helpful count.
     */
    public void decrementHelpfulCount() {
        if (this.helpfulCount != null && this.helpfulCount > 0) {
            this.helpfulCount--;
        }
    }

    /**
     * Increment not helpful count.
     */
    public void incrementNotHelpfulCount() {
        this.notHelpfulCount = (this.notHelpfulCount == null ? 0 : this.notHelpfulCount) + 1;
    }

    /**
     * Decrement not helpful count.
     */
    public void decrementNotHelpfulCount() {
        if (this.notHelpfulCount != null && this.notHelpfulCount > 0) {
            this.notHelpfulCount--;
        }
    }
}
