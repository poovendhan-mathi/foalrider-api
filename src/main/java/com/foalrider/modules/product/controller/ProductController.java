package com.foalrider.modules.product.controller;

import com.foalrider.modules.product.dto.ProductRequest;
import com.foalrider.modules.product.dto.ProductResponse;
import com.foalrider.modules.product.service.ProductService;
import com.foalrider.shared.dto.ApiResponse;
import com.foalrider.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Product controller.
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;

    // ==================== Public Endpoints ====================

    @GetMapping
    @Operation(summary = "Get all active products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProducts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> products = productService.getActiveProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(products), "Products retrieved successfully"));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable UUID productId) {
        productService.incrementViewCount(productId);
        ProductResponse product = productService.getProductById(productId);
        return ResponseEntity.ok(ApiResponse.success(product, "Product retrieved successfully"));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product by slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable String slug) {
        ProductResponse product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(product, "Product retrieved successfully"));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(@PathVariable String sku) {
        ProductResponse product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(product, "Product retrieved successfully"));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProductsByCategory(
            @PathVariable UUID categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductResponse> products = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(products), "Products retrieved successfully"));
    }

    @GetMapping("/brand/{brandId}")
    @Operation(summary = "Get products by brand")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProductsByBrand(
            @PathVariable UUID brandId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductResponse> products = productService.getProductsByBrand(brandId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(products), "Products retrieved successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> searchProducts(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductResponse> products = productService.searchProducts(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(products), "Search results"));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getFeaturedProducts(
            @RequestParam(defaultValue = "8") int limit) {
        List<ProductResponse> products = productService.getFeaturedProducts(limit);
        return ResponseEntity.ok(ApiResponse.success(products, "Featured products retrieved successfully"));
    }

    @GetMapping("/new-arrivals")
    @Operation(summary = "Get new arrivals")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getNewArrivals(
            @RequestParam(defaultValue = "8") int limit) {
        List<ProductResponse> products = productService.getNewArrivals(limit);
        return ResponseEntity.ok(ApiResponse.success(products, "New arrivals retrieved successfully"));
    }

    @GetMapping("/best-sellers")
    @Operation(summary = "Get best sellers")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getBestSellers(
            @RequestParam(defaultValue = "8") int limit) {
        List<ProductResponse> products = productService.getBestSellers(limit);
        return ResponseEntity.ok(ApiResponse.success(products, "Best sellers retrieved successfully"));
    }

    @GetMapping("/on-sale")
    @Operation(summary = "Get products on sale")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getOnSaleProducts(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProductResponse> products = productService.getOnSaleProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(products), "Sale products retrieved successfully"));
    }

    @GetMapping("/{productId}/related")
    @Operation(summary = "Get related products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getRelatedProducts(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "4") int limit) {
        List<ProductResponse> products = productService.getRelatedProducts(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(products, "Related products retrieved successfully"));
    }

    // ==================== Admin Endpoints ====================

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all products including inactive (Admin only)")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getAllProductsAdmin(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductResponse> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(products), "Products retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create product (Admin only)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update product (Admin only)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Product updated successfully"));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete product (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }
}
