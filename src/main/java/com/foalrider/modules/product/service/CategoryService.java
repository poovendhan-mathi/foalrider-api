package com.foalrider.modules.product.service;

import com.foalrider.modules.product.dto.CategoryRequest;
import com.foalrider.modules.product.dto.CategoryResponse;
import com.foalrider.modules.product.entity.Category;

import java.util.List;
import java.util.UUID;

/**
 * Category service interface.
 */
public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(UUID categoryId, CategoryRequest request);

    void deleteCategory(UUID categoryId);

    CategoryResponse getCategoryById(UUID categoryId);

    CategoryResponse getCategoryBySlug(String slug);

    List<CategoryResponse> getAllCategories();

    List<CategoryResponse> getRootCategories();

    List<CategoryResponse> getChildCategories(UUID parentId);

    List<CategoryResponse> getFeaturedCategories();

    List<CategoryResponse> getCategoryTree();

    Category getCategoryEntityById(UUID categoryId);
}
