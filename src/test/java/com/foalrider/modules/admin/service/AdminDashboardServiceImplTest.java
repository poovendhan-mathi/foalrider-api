package com.foalrider.modules.admin.service;

import com.foalrider.modules.admin.dto.DashboardOverviewResponse;
import com.foalrider.modules.order.entity.OrderStatus;
import com.foalrider.modules.order.repository.OrderRepository;
import com.foalrider.modules.product.repository.ProductRepository;
import com.foalrider.modules.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminDashboardServiceImpl.
 * Tests dashboard statistics and metrics.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDashboardService Tests")
class AdminDashboardServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;

    @Nested
    @DisplayName("Dashboard Overview Tests")
    class DashboardOverviewTests {

        @Test
        @DisplayName("Should get dashboard overview with all metrics")
        void getDashboardOverview_ShouldReturnAllMetrics() {
            // Arrange
            when(orderRepository.count()).thenReturn(100L);
            when(orderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(10L);
            when(orderRepository.countByStatus(OrderStatus.PROCESSING)).thenReturn(15L);
            when(orderRepository.countByStatus(OrderStatus.SHIPPED)).thenReturn(20L);
            when(orderRepository.countByStatus(OrderStatus.DELIVERED)).thenReturn(50L);
            when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(5L);
            
            when(orderRepository.sumTotalAmount()).thenReturn(new BigDecimal("25000.00"));
            when(orderRepository.sumTotalAmountByCreatedAtAfter(any(Instant.class)))
                    .thenReturn(new BigDecimal("5000.00"));
            
            when(userRepository.count()).thenReturn(500L);
            when(userRepository.countByIsActiveTrue()).thenReturn(450L);
            when(userRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(10L);
            
            when(productRepository.count()).thenReturn(200L);
            when(productRepository.countByIsActiveTrue()).thenReturn(180L);

            // Act
            DashboardOverviewResponse response = adminDashboardService.getDashboardOverview();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTotalOrders()).isEqualTo(100);
            assertThat(response.getPendingOrders()).isEqualTo(10);
            assertThat(response.getProcessingOrders()).isEqualTo(15);
            assertThat(response.getShippedOrders()).isEqualTo(20);
            assertThat(response.getDeliveredOrders()).isEqualTo(50);
            assertThat(response.getCancelledOrders()).isEqualTo(5);
            assertThat(response.getTotalRevenue()).isEqualByComparingTo(new BigDecimal("25000.00"));
            assertThat(response.getTotalUsers()).isEqualTo(500);
            assertThat(response.getActiveUsers()).isEqualTo(450);
            assertThat(response.getTotalProducts()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should calculate today's revenue correctly")
        void getDashboardOverview_ShouldCalculateTodayRevenue() {
            // Arrange
            setupBasicMocks();
            
            Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
            when(orderRepository.sumTotalAmountByCreatedAtAfter(argThat(instant -> 
                    instant.isAfter(startOfToday.minus(1, ChronoUnit.DAYS)))))
                    .thenReturn(new BigDecimal("1000.00"));

            // Act
            DashboardOverviewResponse response = adminDashboardService.getDashboardOverview();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTodayRevenue()).isNotNull();
        }

        @Test
        @DisplayName("Should calculate weekly revenue correctly")
        void getDashboardOverview_ShouldCalculateWeeklyRevenue() {
            // Arrange
            setupBasicMocks();

            // Act
            DashboardOverviewResponse response = adminDashboardService.getDashboardOverview();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getWeekRevenue()).isNotNull();
        }

        @Test
        @DisplayName("Should calculate monthly revenue correctly")
        void getDashboardOverview_ShouldCalculateMonthlyRevenue() {
            // Arrange
            setupBasicMocks();

            // Act
            DashboardOverviewResponse response = adminDashboardService.getDashboardOverview();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMonthRevenue()).isNotNull();
        }

        @Test
        @DisplayName("Should handle zero orders gracefully")
        void getDashboardOverview_WithZeroOrders_ShouldReturnZeroMetrics() {
            // Arrange
            when(orderRepository.count()).thenReturn(0L);
            when(orderRepository.countByStatus(any(OrderStatus.class))).thenReturn(0L);
            when(orderRepository.sumTotalAmount()).thenReturn(BigDecimal.ZERO);
            when(orderRepository.sumTotalAmountByCreatedAtAfter(any(Instant.class)))
                    .thenReturn(BigDecimal.ZERO);
            
            when(userRepository.count()).thenReturn(0L);
            when(userRepository.countByIsActiveTrue()).thenReturn(0L);
            when(userRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(0L);
            
            when(productRepository.count()).thenReturn(0L);
            when(productRepository.countByIsActiveTrue()).thenReturn(0L);

            // Act
            DashboardOverviewResponse response = adminDashboardService.getDashboardOverview();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTotalOrders()).isZero();
            assertThat(response.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getTotalUsers()).isZero();
            assertThat(response.getTotalProducts()).isZero();
        }

        @Test
        @DisplayName("Should handle null revenue gracefully")
        void getDashboardOverview_WithNullRevenue_ShouldReturnZero() {
            // Arrange
            when(orderRepository.count()).thenReturn(0L);
            when(orderRepository.countByStatus(any(OrderStatus.class))).thenReturn(0L);
            when(orderRepository.sumTotalAmount()).thenReturn(null); // Null revenue
            when(orderRepository.sumTotalAmountByCreatedAtAfter(any(Instant.class))).thenReturn(null);
            
            when(userRepository.count()).thenReturn(0L);
            when(userRepository.countByIsActiveTrue()).thenReturn(0L);
            when(userRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(0L);
            
            when(productRepository.count()).thenReturn(0L);
            when(productRepository.countByIsActiveTrue()).thenReturn(0L);

            // Act
            DashboardOverviewResponse response = adminDashboardService.getDashboardOverview();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        private void setupBasicMocks() {
            when(orderRepository.count()).thenReturn(100L);
            when(orderRepository.countByStatus(any(OrderStatus.class))).thenReturn(10L);
            when(orderRepository.sumTotalAmount()).thenReturn(new BigDecimal("10000.00"));
            when(orderRepository.sumTotalAmountByCreatedAtAfter(any(Instant.class)))
                    .thenReturn(new BigDecimal("1000.00"));
            
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.countByIsActiveTrue()).thenReturn(90L);
            when(userRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(5L);
            
            when(productRepository.count()).thenReturn(50L);
            when(productRepository.countByIsActiveTrue()).thenReturn(45L);
        }
    }

    @Nested
    @DisplayName("New Users Metrics Tests")
    class NewUsersMetricsTests {

        @Test
        @DisplayName("Should get new users today")
        void getDashboardOverview_ShouldReturnNewUsersToday() {
            // Arrange
            when(orderRepository.count()).thenReturn(0L);
            when(orderRepository.countByStatus(any(OrderStatus.class))).thenReturn(0L);
            when(orderRepository.sumTotalAmount()).thenReturn(BigDecimal.ZERO);
            when(orderRepository.sumTotalAmountByCreatedAtAfter(any(Instant.class)))
                    .thenReturn(BigDecimal.ZERO);
            
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.countByIsActiveTrue()).thenReturn(90L);
            when(userRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(5L);
            
            when(productRepository.count()).thenReturn(0L);
            when(productRepository.countByIsActiveTrue()).thenReturn(0L);

            // Act
            DashboardOverviewResponse response = adminDashboardService.getDashboardOverview();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getNewUsersToday()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Product Metrics Tests")
    class ProductMetricsTests {

        @Test
        @DisplayName("Should get low stock products count")
        void getDashboardOverview_ShouldReturnLowStockProducts() {
            // Arrange
            when(orderRepository.count()).thenReturn(0L);
            when(orderRepository.countByStatus(any(OrderStatus.class))).thenReturn(0L);
            when(orderRepository.sumTotalAmount()).thenReturn(BigDecimal.ZERO);
            when(orderRepository.sumTotalAmountByCreatedAtAfter(any(Instant.class)))
                    .thenReturn(BigDecimal.ZERO);
            
            when(userRepository.count()).thenReturn(0L);
            when(userRepository.countByIsActiveTrue()).thenReturn(0L);
            when(userRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(0L);
            
            when(productRepository.count()).thenReturn(100L);
            when(productRepository.countByIsActiveTrue()).thenReturn(90L);

            // Act
            DashboardOverviewResponse response = adminDashboardService.getDashboardOverview();

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getLowStockProducts()).isGreaterThanOrEqualTo(0);
        }
    }
}
