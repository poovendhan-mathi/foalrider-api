package com.foalrider.modules.cart.repository;

import com.foalrider.modules.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId AND (ci.variant.id = :variantId OR (ci.variant IS NULL AND :variantId IS NULL))")
    Optional<CartItem> findByCartIdAndProductIdAndVariantId(
            @Param("cartId") UUID cartId,
            @Param("productId") UUID productId,
            @Param("variantId") UUID variantId);

    Optional<CartItem> findByIdAndCartId(UUID id, UUID cartId);

    void deleteAllByCartId(UUID cartId);
}
