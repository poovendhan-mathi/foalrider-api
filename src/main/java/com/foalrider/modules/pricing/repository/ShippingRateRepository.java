package com.foalrider.modules.pricing.repository;

import com.foalrider.modules.pricing.entity.Region;
import com.foalrider.modules.pricing.entity.ShippingRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShippingRateRepository extends JpaRepository<ShippingRate, UUID> {

    List<ShippingRate> findByRegionAndIsActiveTrueOrderByBaseCostAsc(Region region);

    @Query("SELECT s FROM ShippingRate s WHERE s.region.code = :regionCode AND s.isActive = true ORDER BY s.baseCost ASC")
    List<ShippingRate> findActiveByRegionCode(@Param("regionCode") String regionCode);

    @Query("SELECT s FROM ShippingRate s WHERE s.region.code = :regionCode AND s.shippingMethod = :method AND s.isActive = true")
    Optional<ShippingRate> findByRegionCodeAndMethod(@Param("regionCode") String regionCode, @Param("method") String method);

    Optional<ShippingRate> findByRegionAndIsDefaultTrue(Region region);

    @Query("SELECT s FROM ShippingRate s WHERE s.region.code = :regionCode AND s.isDefault = true AND s.isActive = true")
    Optional<ShippingRate> findDefaultByRegionCode(@Param("regionCode") String regionCode);

    boolean existsByRegionAndShippingMethod(Region region, String shippingMethod);
}
