package com.foalrider.modules.product.controller;

import com.foalrider.modules.product.dto.CategoryRequest;
import com.foalrider.modules.product.dto.CategoryResponse;
import com.foalrider.modules.product.service.CategoryService;
import com.foalrider.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Category controller.
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category management APIs")
public class CategoryController {

    private final CategoryService categoryService;

    // ==================== Public Endpoints ====================

    @GetMapping
    @Operation(summary = "Get all active categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories retrieved successfully"));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get category tree with children")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoryTree() {
        List<CategoryResponse> tree = categoryService.getCategoryTree();
        return ResponseEntity.ok(ApiResponse.success(tree, "Category tree retrieved successfully"));
    }

    @GetMapping("/root")
    @Operation(summary = "Get root categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getRootCategories() {
        List<CategoryResponse> categories = categoryService.getRootCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Root categories retrieved successfully"));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getFeaturedCategories() {
        List<CategoryResponse> categories = categoryService.getFeaturedCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Featured categories retrieved successfully"));
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable UUID categoryId) {
        CategoryResponse category = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(ApiResponse.success(category, "Category retrieved successfully"));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(@PathVariable String slug) {
        CategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category, "Category retrieved successfully"));
    }

    @GetMapping("/{parentId}/children")
    @Operation(summary = "Get child categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getChildCategories(@PathVariable UUID parentId) {
        List<CategoryResponse> categories = categoryService.getChildCategories(parentId);
        return ResponseEntity.ok(ApiResponse.success(categories, "Child categories retrieved successfully"));
    }

    // ==================== Admin Endpoints ====================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create category (Admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(category, "Category created successfully"));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update category (Admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(ApiResponse.success(category, "Category updated successfully"));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete category (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
    }
}
