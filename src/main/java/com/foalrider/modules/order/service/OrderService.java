package com.foalrider.modules.order.service;

import com.foalrider.modules.order.dto.*;
import com.foalrider.modules.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    /**
     * Create a new order from the current user's cart.
     */
    OrderResponse createOrder(CreateOrderRequest request);

    /**
     * Get an order by ID (for the current user).
     */
    OrderResponse getOrderById(UUID orderId);

    /**
     * Get an order by order number (for the current user).
     */
    OrderResponse getOrderByOrderNumber(String orderNumber);

    /**
     * Get all orders for the current user.
     */
    Page<OrderSummaryResponse> getMyOrders(Pageable pageable);

    /**
     * Get orders by status for the current user.
     */
    Page<OrderSummaryResponse> getMyOrdersByStatus(OrderStatus status, Pageable pageable);

    /**
     * Cancel an order (if allowed).
     */
    OrderResponse cancelOrder(UUID orderId);

    // Admin methods

    /**
     * Get all orders (admin).
     */
    Page<OrderSummaryResponse> getAllOrders(Pageable pageable);

    /**
     * Get orders by status (admin).
     */
    Page<OrderSummaryResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);

    /**
     * Get order by ID (admin - any order).
     */
    OrderResponse getOrderByIdAdmin(UUID orderId);

    /**
     * Update order status (admin).
     */
    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);

    /**
     * Add admin note to order.
     */
    OrderResponse addAdminNote(UUID orderId, String note);
}
