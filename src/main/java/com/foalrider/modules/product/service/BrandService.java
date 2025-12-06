package com.foalrider.modules.product.service;

import com.foalrider.modules.product.dto.BrandRequest;
import com.foalrider.modules.product.dto.BrandResponse;
import com.foalrider.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Brand operations.
 */
public interface BrandService {

    /**
     * Get all active brands.
     */
    List<BrandResponse> getAllActiveBrands();

    /**
     * Get all brands with pagination.
     */
    PagedResponse<BrandResponse> getAllBrands(Pageable pageable);

    /**
     * Get featured brands.
     */
    List<BrandResponse> getFeaturedBrands();

    /**
     * Get brand by ID.
     */
    BrandResponse getBrandById(UUID id);

    /**
     * Get brand by slug.
     */
    BrandResponse getBrandBySlug(String slug);

    /**
     * Search brands.
     */
    PagedResponse<BrandResponse> searchBrands(String search, Pageable pageable);

    /**
     * Create a new brand.
     */
    BrandResponse createBrand(BrandRequest request);

    /**
     * Update an existing brand.
     */
    BrandResponse updateBrand(UUID id, BrandRequest request);

    /**
     * Delete a brand.
     */
    void deleteBrand(UUID id);
}
