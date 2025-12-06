package com.foalrider.modules.pricing.repository;

import com.foalrider.modules.pricing.entity.Currency;
import com.foalrider.modules.pricing.entity.Region;
import com.foalrider.modules.pricing.entity.RegionalPrice;
import com.foalrider.modules.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegionalPriceRepository extends JpaRepository<RegionalPrice, UUID> {

    Optional<RegionalPrice> findByProductAndRegion(Product product, Region region);

    @Query("SELECT rp FROM RegionalPrice rp WHERE rp.product.id = :productId AND rp.region.code = :regionCode AND rp.isActive = true")
    Optional<RegionalPrice> findByProductIdAndRegionCode(@Param("productId") UUID productId, @Param("regionCode") String regionCode);

    List<RegionalPrice> findByProductAndIsActiveTrue(Product product);

    @Query("SELECT rp FROM RegionalPrice rp WHERE rp.product.id = :productId AND rp.isActive = true")
    List<RegionalPrice> findActiveByProductId(@Param("productId") UUID productId);

    @Query("SELECT rp FROM RegionalPrice rp WHERE rp.region.code = :regionCode AND rp.isActive = true")
    List<RegionalPrice> findActiveByRegionCode(@Param("regionCode") String regionCode);

    @Query("SELECT rp FROM RegionalPrice rp WHERE rp.product.id IN :productIds AND rp.region.code = :regionCode AND rp.isActive = true")
    List<RegionalPrice> findByProductIdsAndRegionCode(@Param("productIds") List<UUID> productIds, @Param("regionCode") String regionCode);

    boolean existsByProductAndRegion(Product product, Region region);
}
