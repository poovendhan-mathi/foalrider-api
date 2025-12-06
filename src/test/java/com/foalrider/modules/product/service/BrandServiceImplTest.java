package com.foalrider.modules.product.service;

import com.foalrider.modules.product.dto.BrandRequest;
import com.foalrider.modules.product.dto.BrandResponse;
import com.foalrider.modules.product.entity.Brand;
import com.foalrider.modules.product.repository.BrandRepository;
import com.foalrider.shared.dto.PagedResponse;
import com.foalrider.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BrandServiceImpl.
 * Tests brand CRUD operations, search, and listing functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BrandService Tests")
class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private BrandServiceImpl brandService;

    private UUID brandId;
    private Brand testBrand;

    @BeforeEach
    void setUp() {
        brandId = UUID.randomUUID();

        testBrand = Brand.builder()
                .name("Apple")
                .slug("apple")
                .description("Technology company")
                .logoUrl("https://example.com/apple-logo.png")
                .websiteUrl("https://apple.com")
                .isActive(true)
                .isFeatured(true)
                .products(new ArrayList<>())
                .build();
        testBrand.setId(brandId);
    }

    @Nested
    @DisplayName("Create Brand Tests")
    class CreateBrandTests {

        @Test
        @DisplayName("Should create brand successfully")
        void createBrand_WithValidRequest_ShouldSucceed() {
            // Arrange
            BrandRequest request = BrandRequest.builder()
                    .name("Apple")
                    .description("Technology company")
                    .logoUrl("https://example.com/apple-logo.png")
                    .websiteUrl("https://apple.com")
                    .isActive(true)
                    .isFeatured(true)
                    .build();

            when(brandRepository.existsBySlug("apple")).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenReturn(testBrand);

            // Act
            BrandResponse response = brandService.createBrand(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Apple");
            assertThat(response.getSlug()).isEqualTo("apple");
            verify(brandRepository).save(any(Brand.class));
        }

        @Test
        @DisplayName("Should generate unique slug when duplicate exists")
        void createBrand_DuplicateSlug_ShouldGenerateUnique() {
            // Arrange
            BrandRequest request = BrandRequest.builder()
                    .name("Apple")
                    .build();

            when(brandRepository.existsBySlug("apple")).thenReturn(true);
            when(brandRepository.existsBySlug("apple-1")).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> {
                Brand brand = invocation.getArgument(0);
                brand.setId(UUID.randomUUID());
                return brand;
            });

            // Act
            BrandResponse response = brandService.createBrand(request);

            // Assert
            assertThat(response).isNotNull();
            verify(brandRepository).save(argThat(brand -> 
                    "apple-1".equals(brand.getSlug())));
        }

        @Test
        @DisplayName("Should set default values when not provided")
        void createBrand_WithoutOptionalFields_ShouldSetDefaults() {
            // Arrange
            BrandRequest request = BrandRequest.builder()
                    .name("Samsung")
                    .build();

            when(brandRepository.existsBySlug(anyString())).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenAnswer(i -> {
                Brand brand = i.getArgument(0);
                brand.setId(UUID.randomUUID());
                return brand;
            });

            // Act
            brandService.createBrand(request);

            // Assert
            verify(brandRepository).save(argThat(brand -> 
                    brand.getIsActive() && // default true
                    !brand.getIsFeatured())); // default false
        }
    }

    @Nested
    @DisplayName("Update Brand Tests")
    class UpdateBrandTests {

        @Test
        @DisplayName("Should update brand successfully")
        void updateBrand_WithValidRequest_ShouldSucceed() {
            // Arrange
            BrandRequest request = BrandRequest.builder()
                    .name("Apple Inc.")
                    .description("Updated description")
                    .build();

            when(brandRepository.findById(brandId)).thenReturn(Optional.of(testBrand));
            when(brandRepository.existsBySlug(anyString())).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            BrandResponse response = brandService.updateBrand(brandId, request);

            // Assert
            assertThat(response).isNotNull();
            verify(brandRepository).save(any(Brand.class));
        }

        @Test
        @DisplayName("Should throw exception when brand not found")
        void updateBrand_NotFound_ShouldThrowException() {
            // Arrange
            BrandRequest request = BrandRequest.builder()
                    .name("Updated")
                    .build();

            when(brandRepository.findById(brandId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> brandService.updateBrand(brandId, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should update slug when name changes")
        void updateBrand_NameChange_ShouldUpdateSlug() {
            // Arrange
            BrandRequest request = BrandRequest.builder()
                    .name("Apple Inc")
                    .build();

            when(brandRepository.findById(brandId)).thenReturn(Optional.of(testBrand));
            when(brandRepository.existsBySlug("apple-inc")).thenReturn(false);
            when(brandRepository.save(any(Brand.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            BrandResponse response = brandService.updateBrand(brandId, request);

            // Assert
            verify(brandRepository).save(argThat(brand -> 
                    "apple-inc".equals(brand.getSlug())));
        }

        @Test
        @DisplayName("Should update only provided fields")
        void updateBrand_PartialUpdate_ShouldUpdateOnlyProvided() {
            // Arrange
            BrandRequest request = BrandRequest.builder()
                    .description("New description only")
                    .build();

            when(brandRepository.findById(brandId)).thenReturn(Optional.of(testBrand));
            when(brandRepository.save(any(Brand.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            brandService.updateBrand(brandId, request);

            // Assert
            verify(brandRepository).save(argThat(brand -> 
                    "Apple".equals(brand.getName()) && // Name unchanged
                    "New description only".equals(brand.getDescription())));
        }
    }

    @Nested
    @DisplayName("Delete Brand Tests")
    class DeleteBrandTests {

        @Test
        @DisplayName("Should soft delete brand")
        void deleteBrand_ShouldSoftDelete() {
            // Arrange
            when(brandRepository.findById(brandId)).thenReturn(Optional.of(testBrand));
            when(brandRepository.save(any(Brand.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            brandService.deleteBrand(brandId);

            // Assert
            verify(brandRepository).save(argThat(brand -> !brand.getIsActive()));
        }

        @Test
        @DisplayName("Should throw exception when brand not found")
        void deleteBrand_NotFound_ShouldThrowException() {
            // Arrange
            when(brandRepository.findById(brandId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> brandService.deleteBrand(brandId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Brand Tests")
    class GetBrandTests {

        @Test
        @DisplayName("Should get brand by ID")
        void getBrandById_ShouldReturnBrand() {
            // Arrange
            when(brandRepository.findById(brandId)).thenReturn(Optional.of(testBrand));

            // Act
            BrandResponse response = brandService.getBrandById(brandId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(brandId);
            assertThat(response.getName()).isEqualTo("Apple");
        }

        @Test
        @DisplayName("Should throw exception when brand not found by ID")
        void getBrandById_NotFound_ShouldThrowException() {
            // Arrange
            when(brandRepository.findById(brandId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> brandService.getBrandById(brandId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should get brand by slug")
        void getBrandBySlug_ShouldReturnBrand() {
            // Arrange
            when(brandRepository.findBySlug("apple")).thenReturn(Optional.of(testBrand));

            // Act
            BrandResponse response = brandService.getBrandBySlug("apple");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getSlug()).isEqualTo("apple");
        }

        @Test
        @DisplayName("Should throw exception when brand not found by slug")
        void getBrandBySlug_NotFound_ShouldThrowException() {
            // Arrange
            when(brandRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> brandService.getBrandBySlug("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("List Brands Tests")
    class ListBrandsTests {

        @Test
        @DisplayName("Should get all active brands")
        void getAllActiveBrands_ShouldReturnBrands() {
            // Arrange
            when(brandRepository.findByIsActiveTrueOrderByNameAsc())
                    .thenReturn(Arrays.asList(testBrand));

            // Act
            List<BrandResponse> response = brandService.getAllActiveBrands();

            // Assert
            assertThat(response).hasSize(1);
            assertThat(response.get(0).getName()).isEqualTo("Apple");
        }

        @Test
        @DisplayName("Should get all brands with pagination")
        void getAllBrands_WithPagination_ShouldReturnPagedResponse() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Brand> page = new PageImpl<>(Arrays.asList(testBrand), pageable, 1);

            when(brandRepository.findByIsActiveTrue(pageable)).thenReturn(page);

            // Act
            PagedResponse<BrandResponse> response = brandService.getAllBrands(pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get featured brands")
        void getFeaturedBrands_ShouldReturnFeaturedOnly() {
            // Arrange
            when(brandRepository.findFeaturedBrands())
                    .thenReturn(Arrays.asList(testBrand));

            // Act
            List<BrandResponse> response = brandService.getFeaturedBrands();

            // Assert
            assertThat(response).hasSize(1);
            assertThat(response.get(0).getIsFeatured()).isTrue();
        }

        @Test
        @DisplayName("Should return empty list when no brands")
        void getAllActiveBrands_Empty_ShouldReturnEmptyList() {
            // Arrange
            when(brandRepository.findByIsActiveTrueOrderByNameAsc())
                    .thenReturn(Collections.emptyList());

            // Act
            List<BrandResponse> response = brandService.getAllActiveBrands();

            // Assert
            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("Search Brands Tests")
    class SearchBrandsTests {

        @Test
        @DisplayName("Should search brands successfully")
        void searchBrands_ShouldReturnMatchingBrands() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Brand> page = new PageImpl<>(Arrays.asList(testBrand), pageable, 1);

            when(brandRepository.searchBrands("Apple", pageable)).thenReturn(page);

            // Act
            PagedResponse<BrandResponse> response = brandService.searchBrands("Apple", pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getName()).isEqualTo("Apple");
        }

        @Test
        @DisplayName("Should return empty when no matches")
        void searchBrands_NoMatches_ShouldReturnEmpty() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Brand> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(brandRepository.searchBrands("Nonexistent", pageable)).thenReturn(emptyPage);

            // Act
            PagedResponse<BrandResponse> response = brandService.searchBrands("Nonexistent", pageable);

            // Assert
            assertThat(response.getContent()).isEmpty();
            assertThat(response.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("Brand Response Mapping Tests")
    class BrandResponseMappingTests {

        @Test
        @DisplayName("Should include product count in response")
        void mapToResponse_ShouldIncludeProductCount() {
            // Arrange
            when(brandRepository.findById(brandId)).thenReturn(Optional.of(testBrand));

            // Act
            BrandResponse response = brandService.getBrandById(brandId);

            // Assert
            assertThat(response.getProductCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle null products list")
        void mapToResponse_NullProducts_ShouldReturnZeroCount() {
            // Arrange
            testBrand.setProducts(null);
            when(brandRepository.findById(brandId)).thenReturn(Optional.of(testBrand));

            // Act
            BrandResponse response = brandService.getBrandById(brandId);

            // Assert
            assertThat(response.getProductCount()).isEqualTo(0);
        }
    }
}
