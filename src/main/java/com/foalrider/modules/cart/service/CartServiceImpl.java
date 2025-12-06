package com.foalrider.modules.cart.service;

import com.foalrider.modules.pricing.dto.PricingCalculationDTO;
import com.foalrider.modules.pricing.dto.RegionDTO;
import com.foalrider.modules.pricing.service.PricingService;
import com.foalrider.modules.user.entity.User;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.foalrider.modules.cart.dto.*;
import com.foalrider.modules.cart.entity.Cart;
import com.foalrider.modules.cart.entity.CartItem;
import com.foalrider.modules.cart.repository.CartItemRepository;
import com.foalrider.modules.cart.repository.CartRepository;
import com.foalrider.modules.product.entity.Product;
import com.foalrider.modules.product.entity.ProductVariant;
import com.foalrider.modules.product.repository.ProductRepository;
import com.foalrider.modules.product.repository.ProductVariantRepository;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        UUID userId = getCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        User user = cart.getUser();
        return mapToCartResponse(cart, user.getRegionCode(), user.getPreferredCurrency());
    }

    @Override
    public CartResponse addToCart(AddToCartRequest request) {
        UUID userId = getCurrentUserId();
        Cart cart = getOrCreateCart(userId);

        // Validate product exists and is active
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getIsActive()) {
            throw new IllegalArgumentException("Product is not available");
        }

        // Validate variant if provided
        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
            
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new IllegalArgumentException("Variant does not belong to the specified product");
            }
            
            if (!variant.getIsActive()) {
                throw new IllegalArgumentException("Product variant is not available");
            }
        }

        // Check stock availability - variants always have stock, products without variants need check
        int availableStock = getAvailableStock(product, variant);
        if (request.getQuantity() > availableStock) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock");
        }

        // Check if item already exists in cart
        CartItem existingItem = cartItemRepository.findByCartIdAndProductIdAndVariantId(
                cart.getId(), 
                product.getId(), 
                variant != null ? variant.getId() : null
        ).orElse(null);

        if (existingItem != null) {
            // Update quantity
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (newQuantity > availableStock) {
                throw new IllegalArgumentException("Total quantity exceeds available stock");
            }
            existingItem.setQuantity(newQuantity);
            existingItem.calculateTotalPrice();
            cartItemRepository.save(existingItem);
        } else {
            // Create new cart item
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .build();
            cartItem.setUnitPriceFromProduct();
            cart.addItem(cartItem);
            cartItemRepository.save(cartItem);
        }

        cart.recalculateTotals();
        cartRepository.save(cart);

        log.info("Added product {} to cart for user {}", product.getId(), userId);
        return mapToCartResponse(cart, cart.getUser().getRegionCode(), cart.getUser().getPreferredCurrency());
    }

    @Override
    public CartResponse updateCartItem(UUID itemId, UpdateCartItemRequest request) {
        UUID userId = getCurrentUserId();
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem cartItem = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        // Check stock availability
        int availableStock = getAvailableStock(cartItem.getProduct(), cartItem.getVariant());

        if (request.getQuantity() > availableStock) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock");
        }

        cartItem.setQuantity(request.getQuantity());
        cartItem.calculateTotalPrice();
        cartItemRepository.save(cartItem);

        cart.recalculateTotals();
        cartRepository.save(cart);

        log.info("Updated cart item {} quantity to {} for user {}", itemId, request.getQuantity(), userId);
        return mapToCartResponse(cart, cart.getUser().getRegionCode(), cart.getUser().getPreferredCurrency());
    }

    @Override
    public CartResponse removeCartItem(UUID itemId) {
        UUID userId = getCurrentUserId();
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem cartItem = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);
        cartRepository.save(cart);

        log.info("Removed cart item {} for user {}", itemId, userId);
        return mapToCartResponse(cart, cart.getUser().getRegionCode(), cart.getUser().getPreferredCurrency());
    }

    @Override
    public void clearCart() {
        UUID userId = getCurrentUserId();
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElse(null);

        if (cart != null) {
            cart.clearItems();
            cartItemRepository.deleteAllByCartId(cart.getId());
            cartRepository.save(cart);
            log.info("Cleared cart for user {}", userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCartItemCount() {
        UUID userId = getCurrentUserId();
        return cartRepository.findByUserId(userId)
                .map(Cart::getTotalItems)
                .orElse(0);
    }

    // Helper methods

    private UUID getCurrentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse mapToCartResponse(Cart cart, String regionCode, String currencyOverride) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .toList();

        // Handle empty cart
        if (cart.getItems().isEmpty()) {
            RegionDTO region = pricingService.getRegionWithFallback(regionCode);
            return CartResponse.builder()
                    .id(cart.getId())
                    .userId(cart.getUser().getId())
                    .items(itemResponses)
                    .totalItems(0)
                    .regionCode(region.getCode())
                    .regionName(region.getName())
                    .currencyCode(region.getDefaultCurrency().getCode())
                    .currencySymbol(region.getDefaultCurrency().getSymbol())
                    .subtotal(BigDecimal.ZERO)
                    .formattedSubtotal(region.getDefaultCurrency().getSymbol() + "0.00")
                    .tax(BigDecimal.ZERO)
                    .formattedTax(region.getDefaultCurrency().getSymbol() + "0.00")
                    .shipping(BigDecimal.ZERO)
                    .formattedShipping(region.getDefaultCurrency().getSymbol() + "0.00")
                    .total(BigDecimal.ZERO)
                    .formattedTotal(region.getDefaultCurrency().getSymbol() + "0.00")
                    .qualifiesForFreeShipping(false)
                    .build();
        }

        // Build cart items for pricing calculation
        List<PricingService.CartItem> pricingItems = cart.getItems().stream()
                .map(item -> new PricingService.CartItem(item.getProduct().getId(), item.getQuantity()))
                .toList();

        // Calculate pricing using PricingService
        PricingCalculationDTO pricing = pricingService.calculateFullPricing(
                pricingItems,
                regionCode != null ? regionCode : "US",
                "STANDARD", // Default shipping method
                currencyOverride
        );

        // Get first tax name if available
        String taxName = "Tax";
        String taxRate = "";
        if (pricing.getTaxes() != null && !pricing.getTaxes().isEmpty()) {
            taxName = pricing.getTaxes().get(0).getName();
            taxRate = pricing.getTaxes().get(0).getDisplayRate();
        }

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemResponses)
                .totalItems(cart.getTotalItems())
                .regionCode(pricing.getRegionCode())
                .regionName(pricing.getRegionName())
                .currencyCode(pricing.getCurrencyCode())
                .currencySymbol(pricing.getCurrencySymbol())
                .subtotal(pricing.getSubtotal())
                .formattedSubtotal(pricing.getFormattedSubtotal())
                .tax(pricing.getTotalTax())
                .formattedTax(pricing.getFormattedTotalTax())
                .taxName(taxName)
                .taxRate(taxRate)
                .shipping(pricing.getShippingCost())
                .formattedShipping(pricing.getFormattedShippingCost())
                .shippingMethod(pricing.getShippingMethod())
                .deliveryEstimate(pricing.getDeliveryEstimate())
                .qualifiesForFreeShipping(pricing.getQualifiesForFreeShipping())
                .amountToFreeShipping(pricing.getAmountToFreeShipping())
                .total(pricing.getGrandTotal())
                .formattedTotal(pricing.getFormattedGrandTotal())
                .build();
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        Product product = item.getProduct();
        ProductVariant variant = item.getVariant();

        String productImage = product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .orElse(product.getImages().get(0))
                    .getUrl()
                : null;

        int availableStock = getAvailableStock(product, variant);

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .productImage(productImage)
                .variantId(variant != null ? variant.getId() : null)
                .variantName(variant != null ? variant.getName() : null)
                .variantSku(variant != null ? variant.getSku() : null)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .availableStock(availableStock)
                .inStock(availableStock > 0)
                .build();
    }

    /**
     * Get available stock for a product or variant.
     * For products with variants, stock is managed at variant level.
     * For products without variants, we sum up all variant stock or use a default.
     */
    private int getAvailableStock(Product product, ProductVariant variant) {
        if (variant != null) {
            return variant.getStockQuantity();
        }
        // For products without a specific variant, sum all variant stock
        // or return a large number if no variants exist (unlimited stock)
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            return product.getVariants().stream()
                    .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                    .mapToInt(ProductVariant::getStockQuantity)
                    .sum();
        }
        // No variants - assume unlimited stock (or you could set a default)
        return Integer.MAX_VALUE;
    }
}
