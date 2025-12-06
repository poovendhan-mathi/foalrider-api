package com.foalrider.modules.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Category response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private UUID parentId;
    private String parentName;
    private Integer displayOrder;
    private Boolean isActive;
    private Boolean isFeatured;
    private String metaTitle;
    private String metaDescription;
    private List<CategoryResponse> children;
    private Integer productCount;
}
