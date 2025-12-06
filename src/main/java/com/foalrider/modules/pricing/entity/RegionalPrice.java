package com.foalrider.modules.pricing.entity;

import com.foalrider.modules.product.entity.Product;
import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Regional pricing for products - allows different prices for different regions
 * If no regional price exists, falls back to product's base price
 */
@Entity
@Table(name = "regional_prices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "region_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionalPrice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency; // Usually region's default currency

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice; // Optional promotional price

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice; // For margin calculation

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Get the final price (sale price if available, otherwise base price)
     */
    public BigDecimal getFinalPrice() {
        if (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0) {
            return salePrice;
        }
        return basePrice;
    }

    /**
     * Get discount percentage if sale price is set
     */
    public Integer getDiscountPercentage() {
        if (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0 
                && basePrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = basePrice.subtract(salePrice)
                    .divide(basePrice, 2, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            return discount.intValue();
        }
        return 0;
    }

    /**
     * Get formatted price string using currency formatting rules
     */
    public String getFormattedPrice() {
        return currency.format(getFinalPrice());
    }

    public String getFormattedBasePrice() {
        return currency.format(basePrice);
    }
}
