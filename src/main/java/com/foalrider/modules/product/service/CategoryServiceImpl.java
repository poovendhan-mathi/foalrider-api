package com.foalrider.modules.product.service;

import com.foalrider.modules.product.dto.CategoryRequest;
import com.foalrider.modules.product.dto.CategoryResponse;
import com.foalrider.modules.product.entity.Category;
import com.foalrider.modules.product.repository.CategoryRepository;
import com.foalrider.shared.exception.BadRequestException;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.foalrider.shared.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Category service implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with this name already exists");
        }

        String slug = SlugUtils.toSlug(request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            slug = SlugUtils.toSlug(request.getName()) + "-" + System.currentTimeMillis();
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .build();

        if (request.getParentId() != null) {
            Category parent = getCategoryEntityById(request.getParentId());
            category.setParent(parent);
        }

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created: {}", savedCategory.getName());
        return mapToResponse(savedCategory);
    }

    @Override
    public CategoryResponse updateCategory(UUID categoryId, CategoryRequest request) {
        Category category = getCategoryEntityById(categoryId);

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new BadRequestException("Category with this name already exists");
            }
            category.setName(request.getName());
            category.setSlug(SlugUtils.toSlug(request.getName()));
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }
        if (request.getIsFeatured() != null) {
            category.setIsFeatured(request.getIsFeatured());
        }
        if (request.getMetaTitle() != null) {
            category.setMetaTitle(request.getMetaTitle());
        }
        if (request.getMetaDescription() != null) {
            category.setMetaDescription(request.getMetaDescription());
        }
        if (request.getParentId() != null) {
            if (request.getParentId().equals(categoryId)) {
                throw new BadRequestException("Category cannot be its own parent");
            }
            Category parent = getCategoryEntityById(request.getParentId());
            category.setParent(parent);
        }

        Category savedCategory = categoryRepository.save(category);
        log.info("Category updated: {}", savedCategory.getName());
        return mapToResponse(savedCategory);
    }

    @Override
    public void deleteCategory(UUID categoryId) {
        Category category = getCategoryEntityById(categoryId);
        if (!category.getProducts().isEmpty()) {
            throw new BadRequestException("Cannot delete category with associated products");
        }
        if (!category.getChildren().isEmpty()) {
            throw new BadRequestException("Cannot delete category with child categories");
        }
        categoryRepository.delete(category);
        log.info("Category deleted: {}", category.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID categoryId) {
        return mapToResponse(getCategoryEntityById(categoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        return categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildCategories(UUID parentId) {
        return categoryRepository.findByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(parentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getFeaturedCategories() {
        return categoryRepository.findFeaturedCategories()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        return categoryRepository.findAllWithChildren()
                .stream()
                .map(this::mapToResponseWithChildren)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryEntityById(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .isFeatured(category.getIsFeatured())
                .metaTitle(category.getMetaTitle())
                .metaDescription(category.getMetaDescription())
                .productCount(category.getProducts().size())
                .build();
    }

    private CategoryResponse mapToResponseWithChildren(Category category) {
        CategoryResponse response = mapToResponse(category);
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            response.setChildren(category.getChildren().stream()
                    .filter(Category::getIsActive)
                    .map(this::mapToResponseWithChildren)
                    .collect(Collectors.toList()));
        }
        return response;
    }
}
