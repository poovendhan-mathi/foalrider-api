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
public class ShippingRateDTO {

    private UUID id;
    private String regionCode;
    private String shippingMethod;
    private String name;
    private String description;
    private BigDecimal baseCost;
    private BigDecimal costPerKg;
    private BigDecimal freeShippingThreshold;
    private Integer minDeliveryDays;
    private Integer maxDeliveryDays;
    private String deliveryEstimate;
    private Boolean isActive;
    private Boolean isDefault;
    
    // Formatted for display
    private String formattedBaseCost;
    private String formattedFreeThreshold;
}
