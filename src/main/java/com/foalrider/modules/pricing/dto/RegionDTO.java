package com.foalrider.modules.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionDTO {

    private UUID id;
    private String code;
    private String name;
    private String timezone;
    private String localeCode;
    private String dateFormat;
    private Boolean isActive;
    private Boolean isDefault;
    
    // Default currency info
    private CurrencyDTO defaultCurrency;
}
