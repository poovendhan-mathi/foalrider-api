package com.foalrider.modules.cart.entity;

import com.foalrider.shared.entity.BaseEntity;
import com.foalrider.modules.product.entity.Product;
import com.foalrider.modules.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"cart_id", "product_id", "variant_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    // Calculate total price based on quantity and unit price
    @PrePersist
    @PreUpdate
    public void calculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    // Set unit price from product or variant
    public void setUnitPriceFromProduct() {
        if (variant != null) {
            // Variant price = base price + adjustment
            this.unitPrice = variant.getFinalPrice(product.getBasePrice());
        } else if (product != null) {
            // Use effective price (sale price if available, otherwise base price)
            this.unitPrice = product.getEffectivePrice();
        }
        calculateTotalPrice();
    }
}
