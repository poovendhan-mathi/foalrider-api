package com.foalrider.modules.pricing.repository;

import com.foalrider.modules.pricing.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegionRepository extends JpaRepository<Region, UUID> {

    Optional<Region> findByCode(String code);

    Optional<Region> findByIsDefaultTrue();

    List<Region> findByIsActiveTrue();

    @Query("SELECT r FROM Region r WHERE r.isActive = true ORDER BY r.isDefault DESC, r.name ASC")
    List<Region> findAllActiveRegions();

    boolean existsByCode(String code);
}
