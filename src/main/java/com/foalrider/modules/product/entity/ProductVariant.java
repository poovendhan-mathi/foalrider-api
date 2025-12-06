package com.foalrider.modules.product.entity;

import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Product variant entity for size/color variations.
 */
@Entity
@Table(name = "product_variants", indexes = {
    @Index(name = "idx_variant_sku", columnList = "sku"),
    @Index(name = "idx_variant_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "name", length = 100)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();

    @Column(name = "price_adjustment", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(name = "low_stock_threshold")
    @Builder.Default
    private Integer lowStockThreshold = 5;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "weight", precision = 8, scale = 2)
    private BigDecimal weight;

    /**
     * Check if variant is in stock.
     */
    public boolean isInStock() {
        return stockQuantity > 0;
    }

    /**
     * Check if stock is low.
     */
    public boolean isLowStock() {
        return stockQuantity <= lowStockThreshold && stockQuantity > 0;
    }

    /**
     * Get the final price for this variant.
     */
    public BigDecimal getFinalPrice(BigDecimal basePrice) {
        return basePrice.add(priceAdjustment);
    }
}
