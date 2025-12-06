package com.foalrider.modules.product.controller;

import com.foalrider.modules.product.dto.BrandRequest;
import com.foalrider.modules.product.dto.BrandResponse;
import com.foalrider.modules.product.service.BrandService;
import com.foalrider.shared.dto.ApiResponse;
import com.foalrider.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for brand operations.
 */
@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
@Tag(name = "Brands", description = "Brand management endpoints")
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    @Operation(summary = "Get all active brands", description = "Returns all active brands")
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands() {
        List<BrandResponse> brands = brandService.getAllActiveBrands();
        return ResponseEntity.ok(ApiResponse.success(brands, "Brands retrieved successfully"));
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get brands with pagination", description = "Returns brands with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<BrandResponse>>> getBrandsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<BrandResponse> brands = brandService.getAllBrands(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(brands, "Brands retrieved successfully"));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured brands", description = "Returns featured brands")
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getFeaturedBrands() {
        List<BrandResponse> brands = brandService.getFeaturedBrands();
        return ResponseEntity.ok(ApiResponse.success(brands, "Featured brands retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get brand by ID", description = "Returns a brand by its ID")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable UUID id) {
        BrandResponse brand = brandService.getBrandById(id);
        return ResponseEntity.ok(ApiResponse.success(brand, "Brand retrieved successfully"));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get brand by slug", description = "Returns a brand by its slug")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandBySlug(@PathVariable String slug) {
        BrandResponse brand = brandService.getBrandBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(brand, "Brand retrieved successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search brands", description = "Search brands by name")
    public ResponseEntity<ApiResponse<PagedResponse<BrandResponse>>> searchBrands(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        PagedResponse<BrandResponse> brands = brandService.searchBrands(query, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(brands, "Search results retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create brand", description = "Create a new brand (Admin only)")
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(@Valid @RequestBody BrandRequest request) {
        BrandResponse brand = brandService.createBrand(request);
        return ResponseEntity.ok(ApiResponse.success(brand, "Brand created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update brand", description = "Update an existing brand (Admin only)")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
            @PathVariable UUID id,
            @Valid @RequestBody BrandRequest request) {
        BrandResponse brand = brandService.updateBrand(id, request);
        return ResponseEntity.ok(ApiResponse.success(brand, "Brand updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete brand", description = "Delete a brand (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable UUID id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Brand deleted successfully"));
    }
}
