package com.foalrider.modules.product.service;

import com.foalrider.modules.product.dto.BrandRequest;
import com.foalrider.modules.product.dto.BrandResponse;
import com.foalrider.modules.product.entity.Brand;
import com.foalrider.modules.product.repository.BrandRepository;
import com.foalrider.shared.dto.PagedResponse;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.foalrider.shared.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of BrandService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    @Override
    public List<BrandResponse> getAllActiveBrands() {
        return brandRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PagedResponse<BrandResponse> getAllBrands(Pageable pageable) {
        Page<Brand> page = brandRepository.findByIsActiveTrue(pageable);
        return PagedResponse.of(page.map(this::mapToResponse));
    }

    @Override
    public List<BrandResponse> getFeaturedBrands() {
        return brandRepository.findFeaturedBrands()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BrandResponse getBrandById(UUID id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", id));
        return mapToResponse(brand);
    }

    @Override
    public BrandResponse getBrandBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "slug", slug));
        return mapToResponse(brand);
    }

    @Override
    public PagedResponse<BrandResponse> searchBrands(String search, Pageable pageable) {
        Page<Brand> page = brandRepository.searchBrands(search, pageable);
        return PagedResponse.of(page.map(this::mapToResponse));
    }

    @Override
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        log.info("Creating brand: {}", request.getName());
        
        String slug = SlugUtils.toSlug(request.getName());
        
        // Ensure unique slug
        String uniqueSlug = slug;
        int counter = 1;
        while (brandRepository.existsBySlug(uniqueSlug)) {
            uniqueSlug = slug + "-" + counter++;
        }

        Brand brand = Brand.builder()
                .name(request.getName())
                .slug(uniqueSlug)
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .websiteUrl(request.getWebsiteUrl())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .build();

        brand = brandRepository.save(brand);
        log.info("Created brand with ID: {}", brand.getId());
        
        return mapToResponse(brand);
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(UUID id, BrandRequest request) {
        log.info("Updating brand: {}", id);
        
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", id));

        if (request.getName() != null) {
            brand.setName(request.getName());
            // Update slug if name changed
            String newSlug = SlugUtils.toSlug(request.getName());
            if (!newSlug.equals(brand.getSlug())) {
                String uniqueSlug = newSlug;
                int counter = 1;
                while (brandRepository.existsBySlug(uniqueSlug) && !uniqueSlug.equals(brand.getSlug())) {
                    uniqueSlug = newSlug + "-" + counter++;
                }
                brand.setSlug(uniqueSlug);
            }
        }
        if (request.getDescription() != null) {
            brand.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            brand.setLogoUrl(request.getLogoUrl());
        }
        if (request.getWebsiteUrl() != null) {
            brand.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getIsActive() != null) {
            brand.setIsActive(request.getIsActive());
        }
        if (request.getIsFeatured() != null) {
            brand.setIsFeatured(request.getIsFeatured());
        }

        brand = brandRepository.save(brand);
        log.info("Updated brand: {}", id);
        
        return mapToResponse(brand);
    }

    @Override
    @Transactional
    public void deleteBrand(UUID id) {
        log.info("Deleting brand: {}", id);
        
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", id));

        // Soft delete
        brand.setIsActive(false);
        brandRepository.save(brand);
        
        log.info("Deleted brand: {}", id);
    }

    private BrandResponse mapToResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .description(brand.getDescription())
                .logoUrl(brand.getLogoUrl())
                .websiteUrl(brand.getWebsiteUrl())
                .isActive(brand.getIsActive())
                .isFeatured(brand.getIsFeatured())
                .productCount(brand.getProducts() != null ? brand.getProducts().size() : 0)
                .build();
    }
}
