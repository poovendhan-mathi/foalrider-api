package com.foalrider.modules.product.service;

import com.foalrider.modules.product.dto.*;
import com.foalrider.modules.product.entity.*;
import com.foalrider.modules.product.repository.*;
import com.foalrider.shared.exception.BadRequestException;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.foalrider.shared.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Product service implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Product with this SKU already exists");
        }

        String slug = SlugUtils.toSlug(request.getName());
        if (productRepository.existsBySlug(slug)) {
            slug = SlugUtils.toSlug(request.getName()) + "-" + System.currentTimeMillis();
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .sku(request.getSku())
                .shortDescription(request.getShortDescription())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .salePrice(request.getSalePrice())
                .costPrice(request.getCostPrice())
                .category(category)
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .isNew(request.getIsNew() != null ? request.getIsNew() : false)
                .weight(request.getWeight())
                .weightUnit(request.getWeightUnit() != null ? request.getWeightUnit() : "kg")
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .build();

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", request.getBrandId()));
            product.setBrand(brand);
        }

        Product savedProduct = productRepository.save(product);

        // Save images
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            saveProductImages(savedProduct, request.getImages());
        }

        // Save variants
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            saveProductVariants(savedProduct, request.getVariants());
        }

        log.info("Product created: {}", savedProduct.getName());
        return mapToResponse(savedProduct);
    }

    @Override
    public ProductResponse updateProduct(UUID productId, ProductRequest request) {
        Product product = getProductEntityById(productId);

        if (request.getName() != null) {
            product.setName(request.getName());
            product.setSlug(SlugUtils.toSlug(request.getName()));
        }
        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            if (productRepository.existsBySku(request.getSku())) {
                throw new BadRequestException("Product with this SKU already exists");
            }
            product.setSku(request.getSku());
        }
        if (request.getShortDescription() != null) {
            product.setShortDescription(request.getShortDescription());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getBasePrice() != null) {
            product.setBasePrice(request.getBasePrice());
        }
        if (request.getSalePrice() != null) {
            product.setSalePrice(request.getSalePrice());
        }
        if (request.getCostPrice() != null) {
            product.setCostPrice(request.getCostPrice());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        }
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", request.getBrandId()));
            product.setBrand(brand);
        }
        if (request.getTags() != null) {
            product.setTags(request.getTags());
        }
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }
        if (request.getIsFeatured() != null) {
            product.setIsFeatured(request.getIsFeatured());
        }
        if (request.getIsNew() != null) {
            product.setIsNew(request.getIsNew());
        }
        if (request.getWeight() != null) {
            product.setWeight(request.getWeight());
        }
        if (request.getWeightUnit() != null) {
            product.setWeightUnit(request.getWeightUnit());
        }
        if (request.getMetaTitle() != null) {
            product.setMetaTitle(request.getMetaTitle());
        }
        if (request.getMetaDescription() != null) {
            product.setMetaDescription(request.getMetaDescription());
        }

        Product savedProduct = productRepository.save(product);
        log.info("Product updated: {}", savedProduct.getName());
        return mapToResponse(savedProduct);
    }

    @Override
    public void deleteProduct(UUID productId) {
        Product product = getProductEntityById(productId);
        productRepository.delete(product);
        log.info("Product deleted: {}", product.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID productId) {
        return mapToResponse(getProductEntityById(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(UUID categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByBrand(UUID brandId, Pageable pageable) {
        return productRepository.findByBrandIdAndIsActiveTrue(brandId, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
        return productRepository.searchProducts(query, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts(int limit) {
        return productRepository.findFeaturedProducts(PageRequest.of(0, limit))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getNewArrivals(int limit) {
        return productRepository.findNewArrivals(PageRequest.of(0, limit))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getBestSellers(int limit) {
        return productRepository.findBestSellers(PageRequest.of(0, limit))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getOnSaleProducts(Pageable pageable) {
        return productRepository.findOnSaleProducts(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getRelatedProducts(UUID productId, int limit) {
        Product product = getProductEntityById(productId);
        return productRepository.findRelatedProducts(product.getCategory().getId(), productId, PageRequest.of(0, limit))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void incrementViewCount(UUID productId) {
        productRepository.incrementViewCount(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProductEntityById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }

    private void saveProductImages(Product product, List<ProductImageRequest> imageRequests) {
        for (int i = 0; i < imageRequests.size(); i++) {
            ProductImageRequest req = imageRequests.get(i);
            ProductImage image = ProductImage.builder()
                    .product(product)
                    .url(req.getUrl())
                    .altText(req.getAltText())
                    .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : i)
                    .isPrimary(req.getIsPrimary() != null ? req.getIsPrimary() : (i == 0))
                    .build();
            productImageRepository.save(image);
        }
    }

    private void saveProductVariants(Product product, List<ProductVariantRequest> variantRequests) {
        for (ProductVariantRequest req : variantRequests) {
            if (productVariantRepository.existsBySku(req.getSku())) {
                throw new BadRequestException("Variant with SKU " + req.getSku() + " already exists");
            }
            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .sku(req.getSku())
                    .name(req.getName())
                    .attributes(req.getAttributes())
                    .priceAdjustment(req.getPriceAdjustment() != null ? req.getPriceAdjustment() : BigDecimal.ZERO)
                    .stockQuantity(req.getStockQuantity())
                    .lowStockThreshold(req.getLowStockThreshold() != null ? req.getLowStockThreshold() : 5)
                    .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                    .imageUrl(req.getImageUrl())
                    .weight(req.getWeight())
                    .build();
            productVariantRepository.save(variant);
        }
    }

    private ProductResponse mapToResponse(Product product) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        List<ProductVariant> variants = productVariantRepository.findByProductIdAndIsActiveTrue(product.getId());
        Integer totalStock = productVariantRepository.getTotalStockForProduct(product.getId());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .sku(product.getSku())
                .shortDescription(product.getShortDescription())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .salePrice(product.getSalePrice())
                .effectivePrice(product.getEffectivePrice())
                .onSale(product.isOnSale())
                .discountPercentage(product.getDiscountPercentage())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .categorySlug(product.getCategory().getSlug())
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .brandSlug(product.getBrand() != null ? product.getBrand().getSlug() : null)
                .tags(product.getTags())
                .isActive(product.getIsActive())
                .isFeatured(product.getIsFeatured())
                .isNew(product.getIsNew())
                .weight(product.getWeight())
                .weightUnit(product.getWeightUnit())
                .metaTitle(product.getMetaTitle())
                .metaDescription(product.getMetaDescription())
                .viewCount(product.getViewCount())
                .soldCount(product.getSoldCount())
                .avgRating(product.getAvgRating())
                .reviewCount(product.getReviewCount())
                .images(images.stream().map(this::mapImageToResponse).collect(Collectors.toList()))
                .variants(variants.stream().map(v -> mapVariantToResponse(v, product.getBasePrice())).collect(Collectors.toList()))
                .totalStock(totalStock != null ? totalStock : 0)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductImageResponse mapImageToResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .altText(image.getAltText())
                .displayOrder(image.getDisplayOrder())
                .isPrimary(image.getIsPrimary())
                .build();
    }

    private ProductVariantResponse mapVariantToResponse(ProductVariant variant, BigDecimal basePrice) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .name(variant.getName())
                .attributes(variant.getAttributes())
                .priceAdjustment(variant.getPriceAdjustment())
                .finalPrice(variant.getFinalPrice(basePrice))
                .stockQuantity(variant.getStockQuantity())
                .lowStockThreshold(variant.getLowStockThreshold())
                .isActive(variant.getIsActive())
                .inStock(variant.isInStock())
                .lowStock(variant.isLowStock())
                .imageUrl(variant.getImageUrl())
                .weight(variant.getWeight())
                .build();
    }
}
