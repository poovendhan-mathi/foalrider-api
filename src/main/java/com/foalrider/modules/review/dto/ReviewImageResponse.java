package com.foalrider.modules.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for review images.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewImageResponse {

    private UUID id;
    private String url;
    private String altText;
    private Integer sortOrder;
}
