package com.foalrider.modules.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionalPriceDTO {

    private UUID id;
    private UUID productId;
    private String productName;
    private String regionCode;
    private String currencyCode;
    
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private BigDecimal finalPrice;
    private Integer discountPercentage;
    
    // Formatted prices
    private String formattedBasePrice;
    private String formattedSalePrice;
    private String formattedFinalPrice;
    
    private Boolean isActive;
}
