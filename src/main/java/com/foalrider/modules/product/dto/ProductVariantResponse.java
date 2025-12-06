package com.foalrider.modules.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Product variant response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {
    private UUID id;
    private String sku;
    private String name;
    private Map<String, String> attributes;
    private BigDecimal priceAdjustment;
    private BigDecimal finalPrice;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private Boolean isActive;
    private Boolean inStock;
    private Boolean lowStock;
    private String imageUrl;
    private BigDecimal weight;
}
