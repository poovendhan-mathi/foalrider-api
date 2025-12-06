package com.foalrider.modules.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Product response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private UUID id;
    private String name;
    private String slug;
    private String sku;
    private String shortDescription;
    private String description;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private BigDecimal effectivePrice;
    private boolean onSale;
    private Integer discountPercentage;
    
    private UUID categoryId;
    private String categoryName;
    private String categorySlug;
    
    private UUID brandId;
    private String brandName;
    private String brandSlug;
    
    private List<String> tags;
    private Boolean isActive;
    private Boolean isFeatured;
    private Boolean isNew;
    
    private BigDecimal weight;
    private String weightUnit;
    
    private String metaTitle;
    private String metaDescription;
    
    private Integer viewCount;
    private Integer soldCount;
    private BigDecimal avgRating;
    private Integer reviewCount;
    
    private List<ProductImageResponse> images;
    private List<ProductVariantResponse> variants;
    private Integer totalStock;
    
    private Instant createdAt;
    private Instant updatedAt;
}
