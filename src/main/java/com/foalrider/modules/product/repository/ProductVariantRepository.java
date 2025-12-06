package com.foalrider.modules.product.repository;

import com.foalrider.modules.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ProductVariant entity.
 */
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    List<ProductVariant> findByProductIdAndIsActiveTrue(UUID productId);

    @Query("SELECT v FROM ProductVariant v WHERE v.product.id = :productId ORDER BY v.name")
    List<ProductVariant> findByProductId(@Param("productId") UUID productId);

    @Query("SELECT v FROM ProductVariant v WHERE v.stockQuantity <= v.lowStockThreshold AND v.isActive = true")
    List<ProductVariant> findLowStockVariants();

    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity - :quantity WHERE v.id = :variantId AND v.stockQuantity >= :quantity")
    int decrementStock(@Param("variantId") UUID variantId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity + :quantity WHERE v.id = :variantId")
    void incrementStock(@Param("variantId") UUID variantId, @Param("quantity") int quantity);

    @Query("SELECT SUM(v.stockQuantity) FROM ProductVariant v WHERE v.product.id = :productId AND v.isActive = true")
    Integer getTotalStockForProduct(@Param("productId") UUID productId);
}
