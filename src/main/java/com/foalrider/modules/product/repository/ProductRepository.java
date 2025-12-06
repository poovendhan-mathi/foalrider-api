package com.foalrider.modules.product.repository;

import com.foalrider.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Product entity.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySku(String sku);

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    Page<Product> findByCategoryIdAndIsActiveTrue(UUID categoryId, Pageable pageable);

    Page<Product> findByBrandIdAndIsActiveTrue(UUID brandId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isFeatured = true ORDER BY p.createdAt DESC")
    List<Product> findFeaturedProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isNew = true ORDER BY p.createdAt DESC")
    List<Product> findNewArrivals(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.salePrice IS NOT NULL ORDER BY p.createdAt DESC")
    Page<Product> findOnSaleProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.soldCount DESC")
    List<Product> findBestSellers(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(@Param("search") String search, Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :productId")
    void incrementViewCount(@Param("productId") UUID productId);

    @Modifying
    @Query("UPDATE Product p SET p.soldCount = p.soldCount + :quantity WHERE p.id = :productId")
    void incrementSoldCount(@Param("productId") UUID productId, @Param("quantity") int quantity);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.id != :productId AND p.isActive = true ORDER BY p.soldCount DESC")
    List<Product> findRelatedProducts(@Param("categoryId") UUID categoryId, @Param("productId") UUID productId, Pageable pageable);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    long countByIsActiveTrue();

    @Query("SELECT DISTINCT p.tags FROM Product p WHERE p.isActive = true")
    List<List<String>> findAllTags();
}
