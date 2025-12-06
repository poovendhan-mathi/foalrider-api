package com.foalrider.modules.cart.controller;

import com.foalrider.modules.cart.dto.AddToCartRequest;
import com.foalrider.modules.cart.dto.CartResponse;
import com.foalrider.modules.cart.dto.UpdateCartItemRequest;
import com.foalrider.modules.cart.service.CartService;
import com.foalrider.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management endpoints")
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        CartResponse cart = cartService.getCart();
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart retrieved successfully"));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request) {
        CartResponse cart = cartService.addToCart(request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item added to cart successfully"));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse cart = cartService.updateCartItem(itemId, request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart item updated successfully"));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(@PathVariable UUID itemId) {
        CartResponse cart = cartService.removeCartItem(itemId);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item removed from cart successfully"));
    }

    @DeleteMapping
    @Operation(summary = "Clear all items from cart")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok(ApiResponse.success(null, "Cart cleared successfully"));
    }

    @GetMapping("/count")
    @Operation(summary = "Get cart item count")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount() {
        Integer count = cartService.getCartItemCount();
        return ResponseEntity.ok(ApiResponse.success(count, "Cart item count retrieved"));
    }
}
