package com.foalrider.modules.product.service;

import com.foalrider.modules.product.dto.*;
import com.foalrider.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Product service interface.
 */
public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(UUID productId, ProductRequest request);

    void deleteProduct(UUID productId);

    ProductResponse getProductById(UUID productId);

    ProductResponse getProductBySlug(String slug);

    ProductResponse getProductBySku(String sku);

    Page<ProductResponse> getAllProducts(Pageable pageable);

    Page<ProductResponse> getActiveProducts(Pageable pageable);

    Page<ProductResponse> getProductsByCategory(UUID categoryId, Pageable pageable);

    Page<ProductResponse> getProductsByBrand(UUID brandId, Pageable pageable);

    Page<ProductResponse> searchProducts(String query, Pageable pageable);

    List<ProductResponse> getFeaturedProducts(int limit);

    List<ProductResponse> getNewArrivals(int limit);

    List<ProductResponse> getBestSellers(int limit);

    Page<ProductResponse> getOnSaleProducts(Pageable pageable);

    List<ProductResponse> getRelatedProducts(UUID productId, int limit);

    void incrementViewCount(UUID productId);

    Product getProductEntityById(UUID productId);
}
