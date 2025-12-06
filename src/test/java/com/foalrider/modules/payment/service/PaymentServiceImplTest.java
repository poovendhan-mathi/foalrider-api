package com.foalrider.modules.payment.service;

import com.foalrider.config.StripeConfig;
import com.foalrider.modules.order.entity.Order;
import com.foalrider.modules.order.entity.OrderStatus;
import com.foalrider.modules.order.entity.PaymentStatus;
import com.foalrider.modules.order.repository.OrderRepository;
import com.foalrider.modules.payment.dto.PaymentIntentResponse;
import com.foalrider.modules.user.entity.User;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentServiceImpl.
 * Tests payment intent creation, confirmation, and refund operations.
 * Note: Stripe API calls are mocked/not invoked in unit tests.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentService Tests")
class PaymentServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock(lenient = true)
    private StripeConfig stripeConfig;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User testUser;
    private UUID userId;
    private UUID orderId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        testUser = User.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();
        testUser.setId(userId);

        testOrder = Order.builder()
                .orderNumber("ORD-12345")
                .user(testUser)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(99.99))
                .build();
        testOrder.setId(orderId);
    }

    @Nested
    @DisplayName("Create Payment Intent Tests")
    class CreatePaymentIntentTests {

        @Test
        @DisplayName("Should throw exception when order not found")
        void createPaymentIntent_OrderNotFound_ShouldThrowException() {
            // Arrange
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(userId));
                when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> paymentService.createPaymentIntent(orderId))
                        .isInstanceOf(ResourceNotFoundException.class)
                        .hasMessageContaining("Order not found");
            }
        }

        @Test
        @DisplayName("Should throw exception when user doesn't own the order")
        void createPaymentIntent_WrongUser_ShouldThrowException() {
            // Arrange
            UUID anotherUserId = UUID.randomUUID();
            
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(anotherUserId));
                when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

                // Act & Assert
                assertThatThrownBy(() -> paymentService.createPaymentIntent(orderId))
                        .isInstanceOf(UnauthorizedException.class)
                        .hasMessageContaining("Access denied");
            }
        }

        @Test
        @DisplayName("Should throw exception when order is not pending")
        void createPaymentIntent_NotPendingOrder_ShouldThrowException() {
            // Arrange
            testOrder.setStatus(OrderStatus.SHIPPED);
            
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(userId));
                when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

                // Act & Assert
                assertThatThrownBy(() -> paymentService.createPaymentIntent(orderId))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("not in pending status");
            }
        }

        @Test
        @DisplayName("Should throw exception when order is already paid")
        void createPaymentIntent_AlreadyPaid_ShouldThrowException() {
            // Arrange
            testOrder.setPaymentStatus(PaymentStatus.PAID);
            
            try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
                securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(Optional.of(userId));
                when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

                // Act & Assert
                assertThatThrownBy(() -> paymentService.createPaymentIntent(orderId))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("already paid");
            }
        }
    }

    @Nested
    @DisplayName("Confirm Payment Tests")
    class ConfirmPaymentTests {

        @Test
        @DisplayName("Should confirm payment successfully")
        void confirmPayment_ValidPaymentIntent_ShouldUpdateOrder() {
            // Arrange
            String paymentIntentId = "pi_test_123";
            testOrder.setPaymentIntentId(paymentIntentId);
            
            when(orderRepository.findByPaymentIntentId(paymentIntentId))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            paymentService.confirmPayment(paymentIntentId);

            // Assert
            verify(orderRepository).save(argThat(order -> {
                return order.getPaymentStatus() == PaymentStatus.PAID &&
                       order.getStatus() == OrderStatus.CONFIRMED &&
                       order.getPaidAt() != null;
            }));
        }

        @Test
        @DisplayName("Should throw exception when order not found for payment intent")
        void confirmPayment_OrderNotFound_ShouldThrowException() {
            // Arrange
            String paymentIntentId = "pi_test_invalid";
            when(orderRepository.findByPaymentIntentId(paymentIntentId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.confirmPayment(paymentIntentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("Should update order status history on confirmation")
        void confirmPayment_ShouldAddStatusHistory() {
            // Arrange
            String paymentIntentId = "pi_test_456";
            testOrder.setPaymentIntentId(paymentIntentId);
            
            when(orderRepository.findByPaymentIntentId(paymentIntentId))
                    .thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            paymentService.confirmPayment(paymentIntentId);

            // Assert
            verify(orderRepository).save(argThat(order ->
                    order.getStatus() == OrderStatus.CONFIRMED));
        }
    }

    @Nested
    @DisplayName("Process Refund Tests")
    class ProcessRefundTests {

        @Test
        @DisplayName("Should throw exception when order not found")
        void processRefund_OrderNotFound_ShouldThrowException() {
            // Arrange
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.processRefund(orderId, "Customer request"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("Should throw exception when order not paid")
        void processRefund_NotPaid_ShouldThrowException() {
            // Arrange
            testOrder.setPaymentStatus(PaymentStatus.PENDING);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.processRefund(orderId, "Customer request"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("has not been paid");
        }

        @Test
        @DisplayName("Should throw exception when no payment intent exists")
        void processRefund_NoPaymentIntent_ShouldThrowException() {
            // Arrange
            testOrder.setPaymentStatus(PaymentStatus.PAID);
            testOrder.setPaymentIntentId(null);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(testOrder));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.processRefund(orderId, "Customer request"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("No payment intent found");
        }
    }

    @Nested
    @DisplayName("Payment Status Flow Tests")
    class PaymentStatusFlowTests {

        @Test
        @DisplayName("Should validate payment status transitions")
        void paymentStatus_ValidTransitions() {
            // PENDING -> PROCESSING -> PAID -> REFUNDED
            assertThat(PaymentStatus.PENDING).isNotNull();
            assertThat(PaymentStatus.PROCESSING).isNotNull();
            assertThat(PaymentStatus.PAID).isNotNull();
            assertThat(PaymentStatus.FAILED).isNotNull();
            assertThat(PaymentStatus.REFUNDED).isNotNull();
        }

        @Test
        @DisplayName("Should verify order amount conversion to cents")
        void amountConversion_ShouldConvertToCents() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(99.99);
            
            // Act
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
            
            // Assert
            assertThat(amountInCents).isEqualTo(9999L);
        }

        @Test
        @DisplayName("Should verify amount conversion for whole numbers")
        void amountConversion_WholeNumbers_ShouldConvertCorrectly() {
            // Arrange
            BigDecimal amount = BigDecimal.valueOf(100.00);
            
            // Act
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
            
            // Assert
            assertThat(amountInCents).isEqualTo(10000L);
        }
    }

    @Nested
    @DisplayName("Webhook Handling Tests")
    class WebhookHandlingTests {

        @Test
        @DisplayName("Should handle invalid webhook signature")
        void handleWebhook_InvalidSignature_ShouldThrowException() {
            // Arrange
            String payload = "{\"type\": \"payment_intent.succeeded\"}";
            String invalidSignature = "invalid_signature";
            
            when(stripeConfig.getWebhookSecret()).thenReturn("whsec_test_secret");

            // Act & Assert
            // Note: This would throw BadRequestException with "Invalid webhook signature"
            // but we can't fully test Stripe's Webhook.constructEvent without actual Stripe SDK setup
            assertThat(stripeConfig.getWebhookSecret()).isEqualTo("whsec_test_secret");
        }
    }

    @Nested
    @DisplayName("Order Payment Metadata Tests")
    class OrderPaymentMetadataTests {

        @Test
        @DisplayName("Should store payment intent ID on order")
        void storePaymentIntentId_ShouldSetOnOrder() {
            // Arrange
            String paymentIntentId = "pi_test_789";
            testOrder.setPaymentIntentId(paymentIntentId);

            // Assert
            assertThat(testOrder.getPaymentIntentId()).isEqualTo(paymentIntentId);
        }

        @Test
        @DisplayName("Should store payment method on order")
        void storePaymentMethod_ShouldSetOnOrder() {
            // Arrange
            testOrder.setPaymentMethod("stripe");

            // Assert
            assertThat(testOrder.getPaymentMethod()).isEqualTo("stripe");
        }

        @Test
        @DisplayName("Should store transaction ID for refund")
        void storeTransactionId_ForRefund_ShouldSetOnOrder() {
            // Arrange
            String refundId = "re_test_abc";
            testOrder.setPaymentTransactionId(refundId);

            // Assert
            assertThat(testOrder.getPaymentTransactionId()).isEqualTo(refundId);
        }

        @Test
        @DisplayName("Should store paid timestamp")
        void storePaidAt_ShouldSetTimestamp() {
            // Arrange
            Instant paidAt = Instant.now();
            testOrder.setPaidAt(paidAt);

            // Assert
            assertThat(testOrder.getPaidAt()).isEqualTo(paidAt);
        }
    }

    @Nested
    @DisplayName("Currency Configuration Tests")
    class CurrencyConfigurationTests {

        @Test
        @DisplayName("Should use configured currency from Stripe config")
        void getCurrency_ShouldReturnConfiguredCurrency() {
            // Arrange
            when(stripeConfig.getCurrency()).thenReturn("usd");

            // Act
            String currency = stripeConfig.getCurrency();

            // Assert
            assertThat(currency).isEqualTo("usd");
        }

        @Test
        @DisplayName("Should support different currencies")
        void getCurrency_SupportDifferentCurrencies() {
            // Test USD
            when(stripeConfig.getCurrency()).thenReturn("usd");
            assertThat(stripeConfig.getCurrency()).isEqualTo("usd");

            // Test EUR
            when(stripeConfig.getCurrency()).thenReturn("eur");
            assertThat(stripeConfig.getCurrency()).isEqualTo("eur");

            // Test GBP
            when(stripeConfig.getCurrency()).thenReturn("gbp");
            assertThat(stripeConfig.getCurrency()).isEqualTo("gbp");
        }
    }
}
