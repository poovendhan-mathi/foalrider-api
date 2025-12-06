package com.foalrider.modules.product.repository;

import com.foalrider.modules.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ProductImage entity.
 */
@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(UUID productId);

    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(UUID productId);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.product.id = :productId")
    void clearPrimaryImage(@Param("productId") UUID productId);

    @Modifying
    @Query("DELETE FROM ProductImage pi WHERE pi.product.id = :productId")
    void deleteByProductId(@Param("productId") UUID productId);

    int countByProductId(UUID productId);
}
