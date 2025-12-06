package com.foalrider.modules.order.service;

import com.foalrider.modules.cart.entity.Cart;
import com.foalrider.modules.cart.entity.CartItem;
import com.foalrider.modules.cart.repository.CartRepository;
import com.foalrider.modules.order.dto.*;
import com.foalrider.modules.order.entity.*;
import com.foalrider.modules.order.repository.OrderRepository;
import com.foalrider.modules.product.entity.ProductVariant;
import com.foalrider.modules.product.repository.ProductVariantRepository;
import com.foalrider.modules.user.entity.User;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.security.SecurityUtils;
import com.foalrider.shared.exception.BadRequestException;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.foalrider.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;

    // Tax and shipping rates (could be moved to configuration)
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50.00");
    private static final BigDecimal SHIPPING_COST = new BigDecimal("5.99");

    // Order number counter (in production, use database sequence)
    private static final AtomicLong orderCounter = new AtomicLong(System.currentTimeMillis() % 100000);

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get user's cart with items
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Validate stock availability for all items
        validateStockAvailability(cart.getItems());

        // Create order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .customerNotes(request.getCustomerNotes())
                .couponCode(request.getCouponCode())
                .build();

        // Set shipping address
        ShippingAddressRequest shipping = request.getShippingAddress();
        order.setShippingName(shipping.getName());
        order.setShippingPhone(shipping.getPhone());
        order.setShippingEmail(shipping.getEmail());
        order.setShippingAddressLine1(shipping.getAddressLine1());
        order.setShippingAddressLine2(shipping.getAddressLine2());
        order.setShippingCity(shipping.getCity());
        order.setShippingState(shipping.getState());
        order.setShippingPostalCode(shipping.getPostalCode());
        order.setShippingCountry(shipping.getCountry());

        // Set billing address if different
        if (request.getBillingAddress() != null) {
            Map<String, String> billingMap = new HashMap<>();
            BillingAddressRequest billing = request.getBillingAddress();
            billingMap.put("name", billing.getName());
            billingMap.put("phone", billing.getPhone());
            billingMap.put("email", billing.getEmail());
            billingMap.put("addressLine1", billing.getAddressLine1());
            billingMap.put("addressLine2", billing.getAddressLine2());
            billingMap.put("city", billing.getCity());
            billingMap.put("state", billing.getState());
            billingMap.put("postalCode", billing.getPostalCode());
            billingMap.put("country", billing.getCountry());
            order.setBillingAddress(billingMap);
        }

        // Create order items from cart items
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .variant(cartItem.getVariant())
                    .productName(cartItem.getProduct().getName())
                    .productSku(cartItem.getProduct().getSku())
                    .variantName(cartItem.getVariant() != null ? cartItem.getVariant().getName() : null)
                    .variantSku(cartItem.getVariant() != null ? cartItem.getVariant().getSku() : null)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .totalPrice(cartItem.getTotalPrice())
                    .productImageUrl(getProductImageUrl(cartItem))
                    .build();
            order.addItem(orderItem);
        }

        // Calculate totals
        order.calculateTotals();
        BigDecimal tax = order.getSubtotal().multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping_amount = order.getSubtotal().compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : SHIPPING_COST;
        order.setTaxAmount(tax);
        order.setShippingAmount(shipping_amount);
        order.calculateTotals();

        // Add status history
        order.addStatusHistory(OrderStatus.PENDING, "Order created");

        // Save order
        order = orderRepository.save(order);

        // Update stock (reserve items)
        updateStock(cart.getItems(), false);

        // Clear the cart
        cart.clearItems();
        cartRepository.save(cart);

        log.info("Order {} created for user {}", order.getOrderNumber(), userId);
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        UUID userId = getCurrentUserId();
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        UUID userId = getCurrentUserId();
        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(Pageable pageable) {
        UUID userId = getCurrentUserId();
        return orderRepository.findByUserId(userId, pageable)
                .map(this::mapToOrderSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrdersByStatus(OrderStatus status, Pageable pageable) {
        UUID userId = getCurrentUserId();
        return orderRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(this::mapToOrderSummaryResponse);
    }

    @Override
    public OrderResponse cancelOrder(UUID orderId) {
        UUID userId = getCurrentUserId();
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }

        // Can only cancel pending or confirmed orders
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Order cannot be cancelled in its current status");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.addStatusHistory(OrderStatus.CANCELLED, "Order cancelled by customer");

        // Restore stock
        restoreStock(order);

        order = orderRepository.save(order);
        log.info("Order {} cancelled by user {}", order.getOrderNumber(), userId);
        return mapToOrderResponse(order);
    }

    // Admin methods

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::mapToOrderSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable)
                .map(this::mapToOrderSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAdmin(UUID orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToOrderResponse(order);
    }

    @Override
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        // Validate status transition
        validateStatusTransition(oldStatus, newStatus);

        order.setStatus(newStatus);
        order.addStatusHistory(newStatus, request.getNote());

        // Handle status-specific updates
        switch (newStatus) {
            case SHIPPED:
                order.setShippedAt(Instant.now());
                if (request.getTrackingNumber() != null) {
                    order.setTrackingNumber(request.getTrackingNumber());
                }
                break;
            case DELIVERED:
                order.setDeliveredAt(Instant.now());
                break;
            case CANCELLED:
                restoreStock(order);
                break;
            case REFUNDED:
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                restoreStock(order);
                break;
            default:
                break;
        }

        order = orderRepository.save(order);
        log.info("Order {} status updated from {} to {}", order.getOrderNumber(), oldStatus, newStatus);
        return mapToOrderResponse(order);
    }

    @Override
    public OrderResponse addAdminNote(UUID orderId, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        String existingNotes = order.getAdminNotes();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String newNote = String.format("[%s] %s", timestamp, note);

        if (existingNotes != null && !existingNotes.isEmpty()) {
            order.setAdminNotes(existingNotes + "\n" + newNote);
        } else {
            order.setAdminNotes(newNote);
        }

        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    // Helper methods

    private UUID getCurrentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));
    }

    private String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long counter = orderCounter.incrementAndGet();
        return String.format("FR%s%05d", datePart, counter % 100000);
    }

    private void validateStockAvailability(List<CartItem> items) {
        for (CartItem item : items) {
            if (item.getVariant() != null) {
                ProductVariant variant = item.getVariant();
                if (variant.getStockQuantity() < item.getQuantity()) {
                    throw new BadRequestException(
                            String.format("Insufficient stock for %s - %s", 
                                    item.getProduct().getName(), 
                                    variant.getName()));
                }
            }
        }
    }

    private void updateStock(List<CartItem> items, boolean restore) {
        for (CartItem item : items) {
            if (item.getVariant() != null) {
                ProductVariant variant = item.getVariant();
                int currentStock = variant.getStockQuantity();
                int newStock = restore 
                        ? currentStock + item.getQuantity() 
                        : currentStock - item.getQuantity();
                variant.setStockQuantity(Math.max(0, newStock));
                productVariantRepository.save(variant);
            }
        }
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getVariant() != null) {
                ProductVariant variant = item.getVariant();
                variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                productVariantRepository.save(variant);
            }
        }
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        // Define valid transitions
        boolean valid = switch (from) {
            case PENDING -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELLED || to == OrderStatus.FAILED;
            case CONFIRMED -> to == OrderStatus.PROCESSING || to == OrderStatus.CANCELLED;
            case PROCESSING -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELLED;
            case SHIPPED -> to == OrderStatus.DELIVERED;
            case DELIVERED -> to == OrderStatus.COMPLETED || to == OrderStatus.REFUNDED;
            case COMPLETED -> to == OrderStatus.REFUNDED;
            default -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    String.format("Invalid status transition from %s to %s", from, to));
        }
    }

    private String getProductImageUrl(CartItem cartItem) {
        if (cartItem.getProduct().getImages() != null && !cartItem.getProduct().getImages().isEmpty()) {
            return cartItem.getProduct().getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .orElse(cartItem.getProduct().getImages().get(0))
                    .getUrl();
        }
        return null;
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        ShippingAddressResponse shippingAddress = ShippingAddressResponse.builder()
                .name(order.getShippingName())
                .phone(order.getShippingPhone())
                .email(order.getShippingEmail())
                .addressLine1(order.getShippingAddressLine1())
                .addressLine2(order.getShippingAddressLine2())
                .city(order.getShippingCity())
                .state(order.getShippingState())
                .postalCode(order.getShippingPostalCode())
                .country(order.getShippingCountry())
                .build();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .userName(order.getUser().getFirstName() + " " + order.getUser().getLastName())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .shippingAmount(order.getShippingAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(shippingAddress)
                .paymentMethod(order.getPaymentMethod())
                .paidAt(order.getPaidAt())
                .shippingMethod(order.getShippingMethod())
                .trackingNumber(order.getTrackingNumber())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .customerNotes(order.getCustomerNotes())
                .couponCode(order.getCouponCode())
                .items(itemResponses)
                .itemCount(order.getItems().size())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .productSlug(item.getProduct().getSlug())
                .productImageUrl(item.getProductImageUrl())
                .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                .variantName(item.getVariantName())
                .variantSku(item.getVariantSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }

    private OrderSummaryResponse mapToOrderSummaryResponse(Order order) {
        String firstItemName = null;
        String firstItemImage = null;

        if (!order.getItems().isEmpty()) {
            OrderItem firstItem = order.getItems().get(0);
            firstItemName = firstItem.getProductName();
            firstItemImage = firstItem.getProductImageUrl();
        }

        return OrderSummaryResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItems().size())
                .firstItemName(firstItemName)
                .firstItemImage(firstItemImage)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
