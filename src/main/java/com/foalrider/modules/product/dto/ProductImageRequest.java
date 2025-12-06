package com.foalrider.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product image request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageRequest {

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String url;

    @Size(max = 255, message = "Alt text cannot exceed 255 characters")
    private String altText;

    private Integer displayOrder;

    private Boolean isPrimary;
}
