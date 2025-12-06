package com.foalrider.modules.product.service;

import com.foalrider.modules.product.dto.CategoryRequest;
import com.foalrider.modules.product.dto.CategoryResponse;
import com.foalrider.modules.product.entity.Category;
import com.foalrider.modules.product.repository.CategoryRepository;
import com.foalrider.shared.exception.BadRequestException;
import com.foalrider.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategoryServiceImpl.
 * Tests category CRUD operations and hierarchical category management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private UUID categoryId;
    private Category testCategory;
    private Category parentCategory;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();

        testCategory = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Electronic devices and gadgets")
                .displayOrder(1)
                .isActive(true)
                .isFeatured(true)
                .products(new ArrayList<>())
                .children(new ArrayList<>())
                .build();
        testCategory.setId(categoryId);

        parentCategory = Category.builder()
                .name("Parent Category")
                .slug("parent-category")
                .isActive(true)
                .products(new ArrayList<>())
                .children(new ArrayList<>())
                .build();
        parentCategory.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("Create Category Tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create category successfully")
        void createCategory_WithValidRequest_ShouldSucceed() {
            // Arrange
            CategoryRequest request = CategoryRequest.builder()
                    .name("Electronics")
                    .description("Electronic devices")
                    .displayOrder(1)
                    .isActive(true)
                    .isFeatured(true)
                    .build();

            when(categoryRepository.existsByName(anyString())).thenReturn(false);
            when(categoryRepository.existsBySlug(anyString())).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

            // Act
            CategoryResponse response = categoryService.createCategory(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Electronics");
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should throw exception when category name already exists")
        void createCategory_DuplicateName_ShouldThrowException() {
            // Arrange
            CategoryRequest request = CategoryRequest.builder()
                    .name("Electronics")
                    .build();

            when(categoryRepository.existsByName("Electronics")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("name already exists");
        }

        @Test
        @DisplayName("Should generate unique slug when duplicate exists")
        void createCategory_DuplicateSlug_ShouldGenerateUnique() {
            // Arrange
            CategoryRequest request = CategoryRequest.builder()
                    .name("Electronics")
                    .build();

            when(categoryRepository.existsByName(anyString())).thenReturn(false);
            when(categoryRepository.existsBySlug("electronics")).thenReturn(true);
            when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            CategoryResponse response = categoryService.createCategory(request);

            // Assert
            verify(categoryRepository).save(argThat(cat -> 
                    cat.getSlug().startsWith("electronics")));
        }

        @Test
        @DisplayName("Should create category with parent")
        void createCategory_WithParent_ShouldSucceed() {
            // Arrange
            UUID parentId = parentCategory.getId();
            CategoryRequest request = CategoryRequest.builder()
                    .name("Smartphones")
                    .parentId(parentId)
                    .build();

            when(categoryRepository.existsByName(anyString())).thenReturn(false);
            when(categoryRepository.existsBySlug(anyString())).thenReturn(false);
            when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parentCategory));
            when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            CategoryResponse response = categoryService.createCategory(request);

            // Assert
            verify(categoryRepository).save(argThat(cat -> cat.getParent() != null));
        }

        @Test
        @DisplayName("Should set default values when not provided")
        void createCategory_WithoutOptionalFields_ShouldSetDefaults() {
            // Arrange
            CategoryRequest request = CategoryRequest.builder()
                    .name("Electronics")
                    .build();

            when(categoryRepository.existsByName(anyString())).thenReturn(false);
            when(categoryRepository.existsBySlug(anyString())).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            categoryService.createCategory(request);

            // Assert
            verify(categoryRepository).save(argThat(cat -> 
                    cat.getDisplayOrder() == 0 &&
                    cat.getIsActive() &&
                    !cat.getIsFeatured()));
        }
    }

    @Nested
    @DisplayName("Update Category Tests")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Should update category successfully")
        void updateCategory_WithValidRequest_ShouldSucceed() {
            // Arrange
            CategoryRequest request = CategoryRequest.builder()
                    .name("Updated Electronics")
                    .description("Updated description")
                    .build();

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.existsByName("Updated Electronics")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            CategoryResponse response = categoryService.updateCategory(categoryId, request);

            // Assert
            assertThat(response).isNotNull();
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void updateCategory_NotFound_ShouldThrowException() {
            // Arrange
            CategoryRequest request = CategoryRequest.builder()
                    .name("Updated")
                    .build();

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(categoryId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when updating name to existing name")
        void updateCategory_DuplicateName_ShouldThrowException() {
            // Arrange
            CategoryRequest request = CategoryRequest.builder()
                    .name("Existing Category")
                    .build();

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.existsByName("Existing Category")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(categoryId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("name already exists");
        }

        @Test
        @DisplayName("Should throw exception when setting category as its own parent")
        void updateCategory_SelfParent_ShouldThrowException() {
            // Arrange
            CategoryRequest request = CategoryRequest.builder()
                    .parentId(categoryId)
                    .build();

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.updateCategory(categoryId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cannot be its own parent");
        }

        @Test
        @DisplayName("Should update only provided fields")
        void updateCategory_PartialUpdate_ShouldUpdateOnlyProvided() {
            // Arrange
            CategoryRequest request = CategoryRequest.builder()
                    .description("New description only")
                    .build();

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            categoryService.updateCategory(categoryId, request);

            // Assert
            verify(categoryRepository).save(argThat(cat -> 
                    "Electronics".equals(cat.getName()) && // Name unchanged
                    "New description only".equals(cat.getDescription())));
        }
    }

    @Nested
    @DisplayName("Delete Category Tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should delete category successfully")
        void deleteCategory_EmptyCategory_ShouldSucceed() {
            // Arrange
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            doNothing().when(categoryRepository).delete(testCategory);

            // Act
            categoryService.deleteCategory(categoryId);

            // Assert
            verify(categoryRepository).delete(testCategory);
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void deleteCategory_NotFound_ShouldThrowException() {
            // Arrange
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when category has products")
        void deleteCategory_WithProducts_ShouldThrowException() {
            // Arrange
            testCategory.getProducts().add(null); // Simulate having products
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("associated products");
        }

        @Test
        @DisplayName("Should throw exception when category has children")
        void deleteCategory_WithChildren_ShouldThrowException() {
            // Arrange
            testCategory.getChildren().add(parentCategory); // Simulate having children
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("child categories");
        }
    }

    @Nested
    @DisplayName("Get Category Tests")
    class GetCategoryTests {

        @Test
        @DisplayName("Should get category by ID")
        void getCategoryById_ShouldReturnCategory() {
            // Arrange
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));

            // Act
            CategoryResponse response = categoryService.getCategoryById(categoryId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(categoryId);
            assertThat(response.getName()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("Should throw exception when category not found by ID")
        void getCategoryById_NotFound_ShouldThrowException() {
            // Arrange
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.getCategoryById(categoryId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should get category by slug")
        void getCategoryBySlug_ShouldReturnCategory() {
            // Arrange
            when(categoryRepository.findBySlug("electronics")).thenReturn(Optional.of(testCategory));

            // Act
            CategoryResponse response = categoryService.getCategoryBySlug("electronics");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getSlug()).isEqualTo("electronics");
        }

        @Test
        @DisplayName("Should throw exception when category not found by slug")
        void getCategoryBySlug_NotFound_ShouldThrowException() {
            // Arrange
            when(categoryRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.getCategoryBySlug("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("List Categories Tests")
    class ListCategoriesTests {

        @Test
        @DisplayName("Should get all active categories")
        void getAllCategories_ShouldReturnActiveCategories() {
            // Arrange
            when(categoryRepository.findByIsActiveTrueOrderByNameAsc())
                    .thenReturn(Arrays.asList(testCategory));

            // Act
            List<CategoryResponse> response = categoryService.getAllCategories();

            // Assert
            assertThat(response).hasSize(1);
            assertThat(response.get(0).getName()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("Should get root categories")
        void getRootCategories_ShouldReturnOnlyRootCategories() {
            // Arrange
            when(categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc())
                    .thenReturn(Arrays.asList(testCategory));

            // Act
            List<CategoryResponse> response = categoryService.getRootCategories();

            // Assert
            assertThat(response).hasSize(1);
            verify(categoryRepository).findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();
        }

        @Test
        @DisplayName("Should get child categories")
        void getChildCategories_ShouldReturnChildren() {
            // Arrange
            UUID parentId = parentCategory.getId();
            when(categoryRepository.findByParentIdAndIsActiveTrueOrderByDisplayOrderAsc(parentId))
                    .thenReturn(Arrays.asList(testCategory));

            // Act
            List<CategoryResponse> response = categoryService.getChildCategories(parentId);

            // Assert
            assertThat(response).hasSize(1);
        }

        @Test
        @DisplayName("Should get featured categories")
        void getFeaturedCategories_ShouldReturnFeatured() {
            // Arrange
            when(categoryRepository.findFeaturedCategories())
                    .thenReturn(Arrays.asList(testCategory));

            // Act
            List<CategoryResponse> response = categoryService.getFeaturedCategories();

            // Assert
            assertThat(response).hasSize(1);
            assertThat(response.get(0).getIsFeatured()).isTrue();
        }

        @Test
        @DisplayName("Should return empty list when no categories")
        void getAllCategories_Empty_ShouldReturnEmptyList() {
            // Arrange
            when(categoryRepository.findByIsActiveTrueOrderByNameAsc())
                    .thenReturn(Collections.emptyList());

            // Act
            List<CategoryResponse> response = categoryService.getAllCategories();

            // Assert
            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("Category Tree Tests")
    class CategoryTreeTests {

        @Test
        @DisplayName("Should get category tree")
        void getCategoryTree_ShouldReturnTreeStructure() {
            // Arrange
            when(categoryRepository.findAllWithChildren())
                    .thenReturn(Arrays.asList(testCategory));

            // Act
            List<CategoryResponse> response = categoryService.getCategoryTree();

            // Assert
            assertThat(response).hasSize(1);
            verify(categoryRepository).findAllWithChildren();
        }
    }
}
