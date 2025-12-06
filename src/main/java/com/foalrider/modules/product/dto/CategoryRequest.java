package com.foalrider.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Create/Update category request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    private UUID parentId;

    private Integer displayOrder;

    private Boolean isActive;

    private Boolean isFeatured;

    @Size(max = 100, message = "Meta title cannot exceed 100 characters")
    private String metaTitle;

    @Size(max = 255, message = "Meta description cannot exceed 255 characters")
    private String metaDescription;
}
