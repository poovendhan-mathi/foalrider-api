package com.foalrider.modules.pricing.entity;

import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "currencies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Currency extends BaseEntity {

    @Column(nullable = false, unique = true, length = 3)
    private String code; // ISO 4217 code: USD, SGD, INR, GBP, EUR

    @Column(nullable = false)
    private String name; // US Dollar, Singapore Dollar, etc.

    @Column(nullable = false, length = 5)
    private String symbol; // $, S$, ₹, £, €

    @Column(name = "symbol_position", length = 10)
    @Builder.Default
    private String symbolPosition = "BEFORE"; // BEFORE or AFTER

    @Column(name = "decimal_places")
    @Builder.Default
    private Integer decimalPlaces = 2;

    @Column(name = "decimal_separator", length = 1)
    @Builder.Default
    private String decimalSeparator = ".";

    @Column(name = "thousands_separator", length = 1)
    @Builder.Default
    private String thousandsSeparator = ",";

    @Column(name = "exchange_rate_to_usd", precision = 18, scale = 8)
    @Builder.Default
    private BigDecimal exchangeRateToUsd = BigDecimal.ONE; // Rate to convert to USD (base)

    @Column(name = "stripe_multiplier")
    @Builder.Default
    private Integer stripeMultiplier = 100; // Most currencies: 100 (cents), JPY: 1

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Format an amount according to currency rules
     */
    public String format(BigDecimal amount) {
        if (amount == null) return symbol + "0";
        
        String formatted = String.format("%,." + decimalPlaces + "f", amount)
                .replace(",", "TEMP")
                .replace(".", decimalSeparator)
                .replace("TEMP", thousandsSeparator);
        
        return "BEFORE".equals(symbolPosition) 
                ? symbol + formatted 
                : formatted + " " + symbol;
    }

    /**
     * Convert amount to Stripe's smallest unit
     */
    public long toStripeAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(stripeMultiplier)).longValue();
    }

    /**
     * Convert from Stripe's smallest unit to BigDecimal
     */
    public BigDecimal fromStripeAmount(long stripeAmount) {
        return BigDecimal.valueOf(stripeAmount).divide(BigDecimal.valueOf(stripeMultiplier));
    }
}
