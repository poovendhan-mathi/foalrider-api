package com.foalrider.modules.cart.entity;

import com.foalrider.shared.entity.BaseEntity;
import com.foalrider.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "total_items")
    @Builder.Default
    private Integer totalItems = 0;

    @Column(name = "subtotal", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    // Helper methods
    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
        recalculateTotals();
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
        recalculateTotals();
    }

    public void clearItems() {
        items.forEach(item -> item.setCart(null));
        items.clear();
        totalItems = 0;
        subtotal = BigDecimal.ZERO;
    }

    public void recalculateTotals() {
        this.totalItems = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
        this.subtotal = items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
