package com.foalrider.modules.product.service;

import com.foalrider.modules.product.dto.*;
import com.foalrider.modules.product.entity.*;
import com.foalrider.modules.product.repository.*;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductServiceImpl.
 * Tests product CRUD operations, search, and filtering.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductService Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private Category testCategory;
    private Brand testBrand;
    private ProductRequest productRequest;
    private UUID productId;
    private UUID categoryId;
    private UUID brandId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        brandId = UUID.randomUUID();

        // Setup test category
        testCategory = Category.builder()
                .name("Men's Clothing")
                .slug("mens-clothing")
                .description("Men's clothing category")
                .isActive(true)
                .build();
        testCategory.setId(categoryId);

        // Setup test brand
        testBrand = Brand.builder()
                .name("TestBrand")
                .slug("testbrand")
                .isActive(true)
                .build();
        testBrand.setId(brandId);

        // Setup test product
        testProduct = Product.builder()
                .name("Classic T-Shirt")
                .slug("classic-t-shirt")
                .sku("TSH-001")
                .shortDescription("A classic t-shirt")
                .description("A comfortable classic t-shirt made from 100% cotton")
                .basePrice(new BigDecimal("29.99"))
                .salePrice(new BigDecimal("24.99"))
                .costPrice(new BigDecimal("10.00"))
                .category(testCategory)
                .brand(testBrand)
                .isActive(true)
                .isFeatured(false)
                .isNew(true)
                .tags(Arrays.asList("casual", "cotton", "men"))
                .images(new ArrayList<>())
                .variants(new ArrayList<>())
                .build();
        testProduct.setId(productId);

        // Setup product request
        productRequest = ProductRequest.builder()
                .name("New T-Shirt")
                .sku("TSH-002")
                .shortDescription("A new t-shirt")
                .description("A brand new t-shirt")
                .basePrice(new BigDecimal("34.99"))
                .categoryId(categoryId)
                .brandId(brandId)
                .isActive(true)
                .isFeatured(false)
                .build();
    }

    @Nested
    @DisplayName("Create Product Tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully")
        void createProduct_WithValidRequest_ShouldSucceed() {
            // Arrange
            when(productRepository.existsBySku(anyString())).thenReturn(false);
            when(productRepository.existsBySlug(anyString())).thenReturn(false);
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            when(brandRepository.findById(brandId)).thenReturn(Optional.of(testBrand));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                product.setId(UUID.randomUUID());
                return product;
            });

            // Act
            ProductResponse response = productService.createProduct(productRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("New T-Shirt");
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when SKU already exists")
        void createProduct_WithExistingSku_ShouldThrowException() {
            // Arrange
            when(productRepository.existsBySku(anyString())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> productService.createProduct(productRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("SKU already exists");

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void createProduct_WithInvalidCategory_ShouldThrowException() {
            // Arrange
            when(productRepository.existsBySku(anyString())).thenReturn(false);
            when(productRepository.existsBySlug(anyString())).thenReturn(false);
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.createProduct(productRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Should generate unique slug when duplicate exists")
        void createProduct_WithDuplicateSlug_ShouldGenerateUniqueSlug() {
            // Arrange
            when(productRepository.existsBySku(anyString())).thenReturn(false);
            when(productRepository.existsBySlug("new-t-shirt")).thenReturn(true);
            when(productRepository.existsBySlug(startsWith("new-t-shirt-"))).thenReturn(false);
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
            when(brandRepository.findById(brandId)).thenReturn(Optional.of(testBrand));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product product = invocation.getArgument(0);
                product.setId(UUID.randomUUID());
                return product;
            });

            // Act
            ProductResponse response = productService.createProduct(productRequest);

            // Assert
            assertThat(response).isNotNull();
            verify(productRepository).save(argThat(product -> 
                    product.getSlug().startsWith("new-t-shirt-")));
        }
    }

    @Nested
    @DisplayName("Get Product Tests")
    class GetProductTests {

        @Test
        @DisplayName("Should get product by ID successfully")
        void getProductById_WithValidId_ShouldReturnProduct() {
            // Arrange
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

            // Act
            ProductResponse response = productService.getProductById(productId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(productId);
            assertThat(response.getName()).isEqualTo("Classic T-Shirt");
        }

        @Test
        @DisplayName("Should throw exception when product not found by ID")
        void getProductById_WithInvalidId_ShouldThrowException() {
            // Arrange
            UUID invalidId = UUID.randomUUID();
            when(productRepository.findById(invalidId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.getProductById(invalidId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should get product by slug successfully")
        void getProductBySlug_WithValidSlug_ShouldReturnProduct() {
            // Arrange
            when(productRepository.findBySlug("classic-t-shirt"))
                    .thenReturn(Optional.of(testProduct));

            // Act
            ProductResponse response = productService.getProductBySlug("classic-t-shirt");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getSlug()).isEqualTo("classic-t-shirt");
        }

        @Test
        @DisplayName("Should throw exception when product not found by slug")
        void getProductBySlug_WithInvalidSlug_ShouldThrowException() {
            // Arrange
            when(productRepository.findBySlug("invalid-slug"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.getProductBySlug("invalid-slug"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("List Products Tests")
    class ListProductsTests {

        @Test
        @DisplayName("Should get all products with pagination")
        void getAllProducts_WithPagination_ShouldReturnPagedProducts() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Product> products = Arrays.asList(testProduct);
            Page<Product> productPage = new PageImpl<>(products, pageable, 1);
            
            when(productRepository.findAll(pageable)).thenReturn(productPage);

            // Act
            Page<ProductResponse> response = productService.getAllProducts(pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get products by category")
        void getProductsByCategory_WithValidCategory_ShouldReturnProducts() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Product> products = Arrays.asList(testProduct);
            Page<Product> productPage = new PageImpl<>(products, pageable, 1);
            
            when(productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable))
                    .thenReturn(productPage);

            // Act
            Page<ProductResponse> response = productService.getProductsByCategory(categoryId, pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should get featured products")
        void getFeaturedProducts_ShouldReturnFeaturedProducts() {
            // Arrange
            Product featuredProduct = Product.builder()
                    .name("Featured Product")
                    .slug("featured-product")
                    .sku("FEAT-001")
                    .basePrice(new BigDecimal("49.99"))
                    .category(testCategory)
                    .isActive(true)
                    .isFeatured(true)
                    .images(new ArrayList<>())
                    .variants(new ArrayList<>())
                    .build();
            featuredProduct.setId(UUID.randomUUID());
            
            when(productRepository.findFeaturedProducts(any(Pageable.class))).thenReturn(Arrays.asList(featuredProduct));

            // Act
            List<ProductResponse> response = productService.getFeaturedProducts(10);

            // Assert
            assertThat(response).hasSize(1);
            assertThat(response.get(0).getName()).isEqualTo("Featured Product");
        }

        @Test
        @DisplayName("Should get new arrivals")
        void getNewArrivals_ShouldReturnNewProducts() {
            // Arrange
            when(productRepository.findNewArrivals(any(Pageable.class)))
                    .thenReturn(Arrays.asList(testProduct));

            // Act
            List<ProductResponse> response = productService.getNewArrivals(10);

            // Assert
            assertThat(response).hasSize(1);
            assertThat(response.get(0).getIsNew()).isTrue();
        }
    }

    @Nested
    @DisplayName("Search Products Tests")
    class SearchProductsTests {

        @Test
        @DisplayName("Should search products by keyword")
        void searchProducts_WithKeyword_ShouldReturnMatchingProducts() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Product> products = Arrays.asList(testProduct);
            Page<Product> productPage = new PageImpl<>(products, pageable, 1);
            
            when(productRepository.searchProducts("t-shirt", pageable))
                    .thenReturn(productPage);

            // Act
            Page<ProductResponse> response = productService.searchProducts("t-shirt", pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty page for no matches")
        void searchProducts_WithNoMatches_ShouldReturnEmptyPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            
            when(productRepository.searchProducts("nonexistent", pageable))
                    .thenReturn(emptyPage);

            // Act
            Page<ProductResponse> response = productService.searchProducts("nonexistent", pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("Update Product Tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Should update product successfully")
        void updateProduct_WithValidRequest_ShouldSucceed() {
            // Arrange
            ProductRequest updateRequest = ProductRequest.builder()
                    .name("Updated T-Shirt")
                    .basePrice(new BigDecimal("39.99"))
                    .build();

            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // Act
            ProductResponse response = productService.updateProduct(productId, updateRequest);

            // Assert
            assertThat(response).isNotNull();
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when updating with existing SKU")
        void updateProduct_WithExistingSku_ShouldThrowException() {
            // Arrange
            ProductRequest updateRequest = ProductRequest.builder()
                    .sku("EXISTING-SKU")
                    .build();

            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(productRepository.existsBySku("EXISTING-SKU")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> productService.updateProduct(productId, updateRequest))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw exception when product not found for update")
        void updateProduct_WithInvalidId_ShouldThrowException() {
            // Arrange
            UUID invalidId = UUID.randomUUID();
            when(productRepository.findById(invalidId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.updateProduct(invalidId, productRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Product Tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should delete product successfully")
        void deleteProduct_WithValidId_ShouldSucceed() {
            // Arrange
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            doNothing().when(productRepository).delete(any(Product.class));

            // Act
            productService.deleteProduct(productId);

            // Assert
            verify(productRepository).delete(testProduct);
        }

        @Test
        @DisplayName("Should throw exception when product not found for delete")
        void deleteProduct_WithInvalidId_ShouldThrowException() {
            // Arrange
            UUID invalidId = UUID.randomUUID();
            when(productRepository.findById(invalidId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> productService.deleteProduct(invalidId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
