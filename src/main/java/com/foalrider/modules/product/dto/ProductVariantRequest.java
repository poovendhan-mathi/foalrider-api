package com.foalrider.modules.product.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Product variant request DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU cannot exceed 50 characters")
    private String sku;

    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    private Map<String, String> attributes;

    private BigDecimal priceAdjustment;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Min(value = 0, message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold;

    private Boolean isActive;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    @DecimalMin(value = "0.00", message = "Weight cannot be negative")
    private BigDecimal weight;
}
