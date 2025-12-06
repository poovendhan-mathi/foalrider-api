package com.foalrider.modules.product.repository;

import com.foalrider.modules.product.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Brand entity.
 */
@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {

    Optional<Brand> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    List<Brand> findByIsActiveTrueOrderByNameAsc();

    @Query("SELECT b FROM Brand b WHERE b.isActive = true AND b.isFeatured = true ORDER BY b.name")
    List<Brand> findFeaturedBrands();

    Page<Brand> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT b FROM Brand b WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Brand> searchBrands(String search, Pageable pageable);
}
