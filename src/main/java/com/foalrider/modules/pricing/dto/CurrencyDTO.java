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
public class CurrencyDTO {

    private UUID id;
    private String code;
    private String name;
    private String symbol;
    private String symbolPosition;
    private Integer decimalPlaces;
    private String decimalSeparator;
    private String thousandsSeparator;
    private BigDecimal exchangeRateToUsd;
    private Boolean isActive;
    private Boolean isDefault;
}
