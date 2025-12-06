package com.foalrider.modules.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Create/Update product request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name cannot exceed 255 characters")
    private String name;

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU cannot exceed 50 characters")
    private String sku;

    @Size(max = 500, message = "Short description cannot exceed 500 characters")
    private String shortDescription;

    private String description;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be greater than 0")
    private BigDecimal basePrice;

    @DecimalMin(value = "0.01", message = "Sale price must be greater than 0")
    private BigDecimal salePrice;

    @DecimalMin(value = "0.00", message = "Cost price cannot be negative")
    private BigDecimal costPrice;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    private UUID brandId;

    private List<String> tags;

    private Boolean isActive;

    private Boolean isFeatured;

    private Boolean isNew;

    @DecimalMin(value = "0.00", message = "Weight cannot be negative")
    private BigDecimal weight;

    @Size(max = 10)
    private String weightUnit;

    @Size(max = 100)
    private String metaTitle;

    @Size(max = 255)
    private String metaDescription;

    @Valid
    private List<ProductImageRequest> images;

    @Valid
    private List<ProductVariantRequest> variants;
}
