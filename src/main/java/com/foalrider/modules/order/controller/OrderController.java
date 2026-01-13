package com.foalrider.modules.order.controller;

import com.foalrider.modules.order.dto.*;
import com.foalrider.modules.order.entity.OrderStatus;
import com.foalrider.modules.order.service.InvoiceService;
import com.foalrider.modules.order.service.OrderService;
import com.foalrider.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;
    private final InvoiceService invoiceService;

    // Customer endpoints

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new order from cart")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order created successfully"));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's orders")
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getMyOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderSummaryResponse> orders = orderService.getMyOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    @GetMapping("/my/status/{status}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's orders by status")
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getMyOrdersByStatus(
            @PathVariable OrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderSummaryResponse> orders = orderService.getMyOrdersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    @GetMapping("/my/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID orderId) {
        OrderResponse order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order retrieved successfully"));
    }

    @GetMapping("/my/number/{orderNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByOrderNumber(
            @PathVariable String orderNumber) {
        OrderResponse order = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order, "Order retrieved successfully"));
    }

    @PostMapping("/my/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable UUID orderId) {
        OrderResponse order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully"));
    }

    @GetMapping("/my/{orderId}/invoice")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download order invoice as PDF")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable UUID orderId) {
        // Verify the order belongs to the current user first
        orderService.getOrderById(orderId);
        byte[] pdfBytes = invoiceService.generateInvoiceById(orderId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + orderId + ".pdf");
        headers.setContentLength(pdfBytes.length);
        
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @GetMapping("/my/number/{orderNumber}/invoice")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download order invoice by order number")
    public ResponseEntity<byte[]> downloadInvoiceByOrderNumber(@PathVariable String orderNumber) {
        // Verify the order belongs to the current user first
        orderService.getOrderByOrderNumber(orderNumber);
        byte[] pdfBytes = invoiceService.generateInvoiceByOrderNumber(orderNumber);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + orderNumber + ".pdf");
        headers.setContentLength(pdfBytes.length);
        
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    // Admin endpoints

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders (admin)")
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getAllOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderSummaryResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get orders by status (admin)")
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OrderSummaryResponse> orders = orderService.getOrdersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get order by ID (admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByIdAdmin(@PathVariable UUID orderId) {
        OrderResponse order = orderService.getOrderByIdAdmin(orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order retrieved successfully"));
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status (admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse order = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order status updated successfully"));
    }

    @PostMapping("/{orderId}/notes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add admin note to order")
    public ResponseEntity<ApiResponse<OrderResponse>> addAdminNote(
            @PathVariable UUID orderId,
            @RequestBody String note) {
        OrderResponse order = orderService.addAdminNote(orderId, note);
        return ResponseEntity.ok(ApiResponse.success(order, "Note added successfully"));
    }

    @GetMapping("/{orderId}/invoice")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Download order invoice as PDF (admin)")
    public ResponseEntity<byte[]> downloadInvoiceAdmin(@PathVariable UUID orderId) {
        byte[] pdfBytes = invoiceService.generateInvoiceById(orderId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + orderId + ".pdf");
        headers.setContentLength(pdfBytes.length);
        
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
