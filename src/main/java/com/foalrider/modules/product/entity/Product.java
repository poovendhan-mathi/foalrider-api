package com.foalrider.modules.product.entity;

import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Product entity - main product information.
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_slug", columnList = "slug"),
    @Index(name = "idx_product_sku", columnList = "sku"),
    @Index(name = "idx_product_category", columnList = "category_id"),
    @Index(name = "idx_product_brand", columnList = "brand_id"),
    @Index(name = "idx_product_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 280)
    private String slug;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "is_new")
    @Builder.Default
    private Boolean isNew = false;

    @Column(name = "weight", precision = 8, scale = 2)
    private BigDecimal weight;

    @Column(name = "weight_unit", length = 10)
    @Builder.Default
    private String weightUnit = "kg";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dimensions", columnDefinition = "jsonb")
    private Map<String, Object> dimensions;

    // SEO
    @Column(name = "meta_title", length = 100)
    private String metaTitle;

    @Column(name = "meta_description", length = 255)
    private String metaDescription;

    // Stats
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "sold_count")
    @Builder.Default
    private Integer soldCount = 0;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    // Relations
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    /**
     * Get the effective price (sale price if available, otherwise base price).
     */
    public BigDecimal getEffectivePrice() {
        return salePrice != null ? salePrice : basePrice;
    }

    /**
     * Check if product is on sale.
     */
    public boolean isOnSale() {
        return salePrice != null && salePrice.compareTo(basePrice) < 0;
    }

    /**
     * Get discount percentage.
     */
    public int getDiscountPercentage() {
        if (!isOnSale()) {
            return 0;
        }
        return basePrice.subtract(salePrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(basePrice, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }
}
