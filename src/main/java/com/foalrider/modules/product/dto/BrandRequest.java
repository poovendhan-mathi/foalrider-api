package com.foalrider.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create/Update brand request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandRequest {

    @NotBlank(message = "Brand name is required")
    @Size(max = 100, message = "Brand name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 500, message = "Logo URL cannot exceed 500 characters")
    private String logoUrl;

    @Size(max = 500, message = "Website URL cannot exceed 500 characters")
    private String websiteUrl;

    private Boolean isActive;

    private Boolean isFeatured;
}
