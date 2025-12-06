package com.foalrider.modules.cart.service;

import com.foalrider.modules.cart.dto.AddToCartRequest;
import com.foalrider.modules.cart.dto.CartResponse;
import com.foalrider.modules.cart.dto.UpdateCartItemRequest;

import java.util.UUID;

public interface CartService {

    /**
     * Get the current user's cart
     */
    CartResponse getCart();

    /**
     * Add an item to the cart
     */
    CartResponse addToCart(AddToCartRequest request);

    /**
     * Update quantity of a cart item
     */
    CartResponse updateCartItem(UUID itemId, UpdateCartItemRequest request);

    /**
     * Remove an item from the cart
     */
    CartResponse removeCartItem(UUID itemId);

    /**
     * Clear all items from the cart
     */
    void clearCart();

    /**
     * Get cart item count for the current user
     */
    Integer getCartItemCount();
}
