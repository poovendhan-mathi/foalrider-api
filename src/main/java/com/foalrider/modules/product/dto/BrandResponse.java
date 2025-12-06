package com.foalrider.modules.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Brand response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String logoUrl;
    private String websiteUrl;
    private Boolean isActive;
    private Boolean isFeatured;
    private Integer productCount;
}
