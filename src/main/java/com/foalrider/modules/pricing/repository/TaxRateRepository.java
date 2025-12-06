package com.foalrider.modules.pricing.repository;

import com.foalrider.modules.pricing.entity.Region;
import com.foalrider.modules.pricing.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, UUID> {

    List<TaxRate> findByRegionAndIsActiveTrueOrderByPriorityAsc(Region region);

    @Query("SELECT t FROM TaxRate t WHERE t.region.code = :regionCode AND t.isActive = true ORDER BY t.priority ASC")
    List<TaxRate> findActiveByRegionCode(@Param("regionCode") String regionCode);

    @Query("SELECT t FROM TaxRate t WHERE t.region.id = :regionId AND t.isActive = true ORDER BY t.priority ASC")
    List<TaxRate> findActiveByRegionId(@Param("regionId") UUID regionId);

    boolean existsByRegionAndName(Region region, String name);
}
