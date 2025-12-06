package com.foalrider.modules.pricing.entity;

import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tax_rates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxRate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(nullable = false)
    private String name; // GST, VAT, Sales Tax, etc.

    @Column(length = 100)
    private String description;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal rate; // 0.07 for 7%, 0.18 for 18%, 0.20 for 20%

    @Column(name = "display_rate")
    private String displayRate; // "7%", "18%", "20%"

    @Column(name = "is_inclusive")
    @Builder.Default
    private Boolean isInclusive = false; // true for VAT (UK), false for GST add-on

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0; // For multiple taxes in same region

    /**
     * Calculate tax amount for a given base amount
     */
    public BigDecimal calculateTax(BigDecimal baseAmount) {
        if (isInclusive) {
            // Extract tax from inclusive price: tax = price - (price / (1 + rate))
            return baseAmount.subtract(
                    baseAmount.divide(BigDecimal.ONE.add(rate), 2, java.math.RoundingMode.HALF_UP)
            );
        } else {
            // Add tax to exclusive price
            return baseAmount.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }
}
