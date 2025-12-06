package com.foalrider.modules.pricing.entity;

import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "shipping_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"region_id", "shipping_method"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingRate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "shipping_method", nullable = false)
    private String shippingMethod; // STANDARD, EXPRESS, OVERNIGHT, FREE

    @Column(nullable = false)
    private String name; // Standard Shipping, Express Delivery, etc.

    @Column
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseCost; // Base shipping cost

    @Column(name = "cost_per_kg", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costPerKg = BigDecimal.ZERO; // Additional cost per kg

    @Column(name = "free_shipping_threshold", precision = 10, scale = 2)
    private BigDecimal freeShippingThreshold; // Minimum order for free shipping

    @Column(name = "min_delivery_days")
    private Integer minDeliveryDays;

    @Column(name = "max_delivery_days")
    private Integer maxDeliveryDays;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Calculate shipping cost based on order total and weight
     */
    public BigDecimal calculateShippingCost(BigDecimal orderTotal, BigDecimal totalWeightKg) {
        // Free shipping if threshold met
        if (freeShippingThreshold != null && orderTotal.compareTo(freeShippingThreshold) >= 0) {
            return BigDecimal.ZERO;
        }

        // Base cost + weight-based cost
        BigDecimal cost = baseCost;
        if (costPerKg != null && totalWeightKg != null && totalWeightKg.compareTo(BigDecimal.ZERO) > 0) {
            cost = cost.add(costPerKg.multiply(totalWeightKg));
        }

        return cost.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public String getDeliveryEstimate() {
        if (minDeliveryDays == null || maxDeliveryDays == null) {
            return "Contact for estimate";
        }
        if (minDeliveryDays.equals(maxDeliveryDays)) {
            return minDeliveryDays + " business days";
        }
        return minDeliveryDays + "-" + maxDeliveryDays + " business days";
    }
}
