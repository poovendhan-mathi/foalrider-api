package com.foalrider.modules.review.entity;

import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * ReviewImage entity - Images attached to reviews.
 */
@Entity
@Table(name = "review_images", indexes = {
    @Index(name = "idx_review_image_review", columnList = "review_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
