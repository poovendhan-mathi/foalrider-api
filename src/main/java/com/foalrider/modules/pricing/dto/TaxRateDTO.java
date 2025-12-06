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
public class TaxRateDTO {

    private UUID id;
    private String regionCode;
    private String name;
    private String description;
    private BigDecimal rate;
    private String displayRate;
    private Boolean isInclusive;
    private Boolean isActive;
    private Integer priority;
}
