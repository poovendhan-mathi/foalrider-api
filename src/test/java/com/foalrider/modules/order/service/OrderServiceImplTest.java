package com.foalrider.modules.order.service;

import com.foalrider.modules.cart.entity.Cart;
import com.foalrider.modules.cart.entity.CartItem;
import com.foalrider.modules.cart.repository.CartRepository;
import com.foalrider.modules.order.dto.*;
import com.foalrider.modules.order.entity.Order;
import com.foalrider.modules.order.entity.OrderItem;
import com.foalrider.modules.order.entity.OrderStatus;
import com.foalrider.modules.order.entity.PaymentStatus;
import com.foalrider.modules.order.repository.OrderRepository;
import com.foalrider.modules.product.entity.Product;
import com.foalrider.modules.product.entity.ProductVariant;
import com.foalrider.modules.product.repository.ProductVariantRepository;
import com.foalrider.modules.user.entity.Role;
import com.foalrider.modules.user.entity.User;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.security.SecurityUtils;
import com.foalrider.shared.exception.BadRequestException;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.foalrider.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
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
 * Unit tests for OrderServiceImpl.
 * Tests order creation, retrieval, status updates, and cancellation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Cart testCart;
    private Product testProduct;
    private ProductVariant testVariant;
    private Order testOrder;
    private CreateOrderRequest createOrderRequest;
    private UUID userId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        // Setup test user
        Role customerRole = Role.builder()
                .id(UUID.randomUUID())
                .name("CUSTOMER")
                .build();

        testUser = User.builder()
                .id(userId)
                .email("customer@test.com")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .role(customerRole)
                .isActive(true)
                .build();

        // Setup test product
        testProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("Test Product")
                .slug("test-product")
                .sku("TST-001")
                .basePrice(new BigDecimal("29.99"))
                .isActive(true)
                .images(new ArrayList<>())
                .build();

        // Setup test variant
        testVariant = ProductVariant.builder()
                .id(UUID.randomUUID())
                .product(testProduct)
                .name("Size M")
                .sku("TST-001-M")
                .price(new BigDecimal("29.99"))
                .stockQuantity(100)
                .isActive(true)
                .build();

        // Setup cart item
        CartItem cartItem = CartItem.builder()
                .id(UUID.randomUUID())
                .product(testProduct)
                .variant(testVariant)
                .quantity(2)
                .unitPrice(new BigDecimal("29.99"))
                .totalPrice(new BigDecimal("59.98"))
                .build();

        // Setup test cart
        testCart = Cart.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .items(new ArrayList<>(Arrays.asList(cartItem)))
                .totalItems(2)
                .subtotal(new BigDecimal("59.98"))
                .build();
        cartItem.setCart(testCart);

        // Setup test order
        testOrder = Order.builder()
                .id(orderId)
                .orderNumber("FR2025120612345")
                .user(testUser)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(new BigDecimal("59.98"))
                .taxAmount(new BigDecimal("4.80"))
                .shippingAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("64.78"))
                .items(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .build();

        // Setup create order request
        ShippingAddressRequest shippingAddress = ShippingAddressRequest.builder()
                .name("John Doe")
                .phone("+1234567890")
                .email("john@test.com")
                .addressLine1("123 Main St")
                .city("San Francisco")
                .state("CA")
                .postalCode("94102")
                .country("USA")
                .build();

        createOrderRequest = CreateOrderRequest.builder()
                .shippingAddress(shippingAddress)
                .customerNotes("Please deliver to front door")
                .build();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully")
        void createOrder_WithValidCart_ShouldSucceed() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
                
                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(testCart));
                when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(orderId);
                    return order;
                });
                when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

                // Act
                OrderResponse response = orderService.createOrder(createOrderRequest);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
                verify(orderRepository).save(any(Order.class));
                verify(cartRepository).save(any(Cart.class)); // Cart should be cleared
            }
        }

        @Test
        @DisplayName("Should throw exception when cart is empty")
        void createOrder_WithEmptyCart_ShouldThrowException() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
                testCart.setItems(new ArrayList<>());
                
                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(testCart));

                // Act & Assert
                assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("Cart is empty");
            }
        }

        @Test
        @DisplayName("Should throw exception when cart not found")
        void createOrder_WithNoCart_ShouldThrowException() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
                
                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                        .isInstanceOf(BadRequestException.class);
            }
        }

        @Test
        @DisplayName("Should calculate totals correctly")
        void createOrder_ShouldCalculateTotalsCorrectly() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
                
                when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
                when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(testCart));
                when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    order.setId(orderId);
                    return order;
                });
                when(cartRepository.save(any(Cart.class))).thenReturn(testCart);

                // Act
                OrderResponse response = orderService.createOrder(createOrderRequest);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getSubtotal()).isNotNull();
                assertThat(response.getTotalAmount()).isGreaterThan(response.getSubtotal());
            }
        }
    }

    @Nested
    @DisplayName("Get Order Tests")
    class GetOrderTests {

        @Test
        @DisplayName("Should get order by ID successfully")
        void getOrderById_WithValidId_ShouldReturnOrder() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
                when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(testOrder));

                // Act
                OrderResponse response = orderService.getOrderById(orderId);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(orderId);
            }
        }

        @Test
        @DisplayName("Should throw exception when order not found")
        void getOrderById_WithInvalidId_ShouldThrowException() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
                UUID invalidId = UUID.randomUUID();
                when(orderRepository.findByIdWithItems(invalidId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> orderService.getOrderById(invalidId))
                        .isInstanceOf(ResourceNotFoundException.class);
            }
        }

        @Test
        @DisplayName("Should throw exception when accessing another user's order")
        void getOrderById_WithWrongUser_ShouldThrowException() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                UUID anotherUserId = UUID.randomUUID();
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(anotherUserId);
                when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(testOrder));

                // Act & Assert
                assertThatThrownBy(() -> orderService.getOrderById(orderId))
                        .isInstanceOf(UnauthorizedException.class)
                        .hasMessageContaining("Access denied");
            }
        }

        @Test
        @DisplayName("Should get order by order number successfully")
        void getOrderByOrderNumber_WithValidNumber_ShouldReturnOrder() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
                when(orderRepository.findByOrderNumberWithItems("FR2025120612345"))
                        .thenReturn(Optional.of(testOrder));

                // Act
                OrderResponse response = orderService.getOrderByOrderNumber("FR2025120612345");

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getOrderNumber()).isEqualTo("FR2025120612345");
            }
        }
    }

    @Nested
    @DisplayName("Get My Orders Tests")
    class GetMyOrdersTests {

        @Test
        @DisplayName("Should get all user orders with pagination")
        void getMyOrders_ShouldReturnPagedOrders() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
                Pageable pageable = PageRequest.of(0, 10);
                Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder), pageable, 1);
                
                when(orderRepository.findByUserId(userId, pageable)).thenReturn(orderPage);

                // Act
                Page<OrderSummaryResponse> response = orderService.getMyOrders(pageable);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getContent()).hasSize(1);
                assertThat(response.getTotalElements()).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("Should get orders by status")
        void getMyOrdersByStatus_ShouldReturnFilteredOrders() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
                Pageable pageable = PageRequest.of(0, 10);
                Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder), pageable, 1);
                
                when(orderRepository.findByUserIdAndStatus(userId, OrderStatus.PENDING, pageable))
                        .thenReturn(orderPage);

                // Act
                Page<OrderSummaryResponse> response = orderService.getMyOrdersByStatus(
                        OrderStatus.PENDING, pageable);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getContent()).hasSize(1);
            }
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel pending order successfully")
        void cancelOrder_WithPendingOrder_ShouldSucceed() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
                testOrder.setStatus(OrderStatus.PENDING);
                
                when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(testOrder));
                when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

                // Act
                OrderResponse response = orderService.cancelOrder(orderId);

                // Assert
                assertThat(response).isNotNull();
                verify(orderRepository).save(argThat(order -> 
                        order.getStatus() == OrderStatus.CANCELLED));
            }
        }

        @Test
        @DisplayName("Should throw exception when cancelling shipped order")
        void cancelOrder_WithShippedOrder_ShouldThrowException() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
                testOrder.setStatus(OrderStatus.SHIPPED);
                
                when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(testOrder));

                // Act & Assert
                assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("cannot be cancelled");
            }
        }

        @Test
        @DisplayName("Should throw exception when cancelling delivered order")
        void cancelOrder_WithDeliveredOrder_ShouldThrowException() {
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                // Arrange
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
                testOrder.setStatus(OrderStatus.DELIVERED);
                
                when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(testOrder));

                // Act & Assert
                assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                        .isInstanceOf(BadRequestException.class);
            }
        }
    }

    @Nested
    @DisplayName("Update Order Status Tests (Admin)")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("Should update order status from PENDING to CONFIRMED")
        void updateOrderStatus_FromPendingToConfirmed_ShouldSucceed() {
            // Arrange
            testOrder.setStatus(OrderStatus.PENDING);
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.CONFIRMED)
                    .build();

            when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // Act
            OrderResponse response = orderService.updateOrderStatus(orderId, request);

            // Assert
            assertThat(response).isNotNull();
            verify(orderRepository).save(argThat(order -> 
                    order.getStatus() == OrderStatus.CONFIRMED));
        }

        @Test
        @DisplayName("Should update order status from CONFIRMED to PROCESSING")
        void updateOrderStatus_FromConfirmedToProcessing_ShouldSucceed() {
            // Arrange
            testOrder.setStatus(OrderStatus.CONFIRMED);
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.PROCESSING)
                    .build();

            when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // Act
            OrderResponse response = orderService.updateOrderStatus(orderId, request);

            // Assert
            assertThat(response).isNotNull();
            verify(orderRepository).save(argThat(order -> 
                    order.getStatus() == OrderStatus.PROCESSING));
        }

        @Test
        @DisplayName("Should throw exception for invalid status transition")
        void updateOrderStatus_WithInvalidTransition_ShouldThrowException() {
            // Arrange
            testOrder.setStatus(OrderStatus.PENDING);
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.PROCESSING) // Invalid: PENDING cannot go directly to PROCESSING
                    .build();

            when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(testOrder));

            // Act & Assert
            assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid status transition");
        }

        @Test
        @DisplayName("Should update order status from PROCESSING to SHIPPED")
        void updateOrderStatus_FromProcessingToShipped_ShouldSucceed() {
            // Arrange
            testOrder.setStatus(OrderStatus.PROCESSING);
            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.SHIPPED)
                    .trackingNumber("TRACK123456")
                    .build();

            when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

            // Act
            OrderResponse response = orderService.updateOrderStatus(orderId, request);

            // Assert
            assertThat(response).isNotNull();
            verify(orderRepository).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("Get All Orders Tests (Admin)")
    class GetAllOrdersTests {

        @Test
        @DisplayName("Should get all orders for admin")
        void getAllOrders_ShouldReturnAllOrders() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder), pageable, 1);
            
            when(orderRepository.findAll(pageable)).thenReturn(orderPage);

            // Act
            Page<OrderSummaryResponse> response = orderService.getAllOrders(pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should get orders by status for admin")
        void getOrdersByStatus_ShouldReturnFilteredOrders() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder), pageable, 1);
            
            when(orderRepository.findByStatus(OrderStatus.PENDING, pageable)).thenReturn(orderPage);

            // Act
            Page<OrderSummaryResponse> response = orderService.getOrdersByStatus(
                    OrderStatus.PENDING, pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
        }
    }
}
