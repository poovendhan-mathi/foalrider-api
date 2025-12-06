package com.foalrider.modules.admin.service;

import com.foalrider.modules.admin.dto.*;
import com.foalrider.modules.order.entity.Order;
import com.foalrider.modules.order.entity.OrderStatus;
import com.foalrider.modules.order.repository.OrderRepository;
import com.foalrider.modules.product.entity.Product;
import com.foalrider.modules.product.entity.ProductVariant;
import com.foalrider.modules.product.repository.BrandRepository;
import com.foalrider.modules.product.repository.CategoryRepository;
import com.foalrider.modules.product.repository.ProductRepository;
import com.foalrider.modules.product.repository.ProductVariantRepository;
import com.foalrider.modules.review.entity.ReviewStatus;
import com.foalrider.modules.review.repository.ReviewRepository;
import com.foalrider.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of AdminDashboardService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ReviewRepository reviewRepository;

    private static final int LOW_STOCK_THRESHOLD = 10;

    @Override
    public DashboardOverviewResponse getDashboardOverview() {
        Instant now = Instant.now();
        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfWeek = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfMonth = LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Order counts by status
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING, Pageable.unpaged()).getTotalElements();
        long processingOrders = orderRepository.findByStatus(OrderStatus.PROCESSING, Pageable.unpaged()).getTotalElements();
        long shippedOrders = orderRepository.findByStatus(OrderStatus.SHIPPED, Pageable.unpaged()).getTotalElements();
        long deliveredOrders = orderRepository.findByStatus(OrderStatus.DELIVERED, Pageable.unpaged()).getTotalElements();
        long cancelledOrders = orderRepository.findByStatus(OrderStatus.CANCELLED, Pageable.unpaged()).getTotalElements();

        // Revenue calculations
        List<Order> allOrders = orderRepository.findAll();
        BigDecimal totalRevenue = calculateRevenue(allOrders);
        
        List<Order> todayOrders = orderRepository.findByDateRange(startOfToday, now);
        BigDecimal todayRevenue = calculateRevenue(todayOrders);
        
        List<Order> weekOrders = orderRepository.findByDateRange(startOfWeek, now);
        BigDecimal weekRevenue = calculateRevenue(weekOrders);
        
        List<Order> monthOrders = orderRepository.findByDateRange(startOfMonth, now);
        BigDecimal monthRevenue = calculateRevenue(monthOrders);

        // User counts
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();

        // Count new users (simplified - would need proper date queries in production)
        long newUsersToday = 0;
        long newUsersWeek = 0;
        long newUsersMonth = 0;

        // Product counts
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByIsActiveTrue();
        
        // Stock counts from variants
        List<ProductVariant> allVariants = variantRepository.findAll();
        long outOfStockProducts = allVariants.stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() == 0)
                .map(v -> v.getProduct().getId())
                .distinct()
                .count();
        long lowStockProducts = allVariants.stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0 && v.getStockQuantity() <= LOW_STOCK_THRESHOLD)
                .map(v -> v.getProduct().getId())
                .distinct()
                .count();

        // Review counts
        long totalReviews = reviewRepository.count();
        long pendingReviews = reviewRepository.findByStatus(ReviewStatus.PENDING, Pageable.unpaged()).getTotalElements();
        
        // Average rating from all approved reviews
        BigDecimal averageRating = BigDecimal.ZERO;

        // Category and brand counts
        long totalCategories = categoryRepository.count();
        long totalBrands = brandRepository.count();

        return DashboardOverviewResponse.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .processingOrders(processingOrders)
                .shippedOrders(shippedOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .weekRevenue(weekRevenue)
                .monthRevenue(monthRevenue)
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .newUsersToday(newUsersToday)
                .newUsersWeek(newUsersWeek)
                .newUsersMonth(newUsersMonth)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .outOfStockProducts(outOfStockProducts)
                .lowStockProducts(lowStockProducts)
                .totalReviews(totalReviews)
                .pendingReviews(pendingReviews)
                .averageRating(averageRating)
                .totalCategories(totalCategories)
                .totalBrands(totalBrands)
                .build();
    }

    @Override
    public SalesReportResponse getSalesReport(LocalDate startDate, LocalDate endDate) {
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Order> orders = orderRepository.findByDateRange(startInstant, endInstant);

        // Filter out cancelled orders for revenue
        List<Order> validOrders = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());

        BigDecimal totalRevenue = calculateRevenue(validOrders);
        long totalItemsSold = validOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .mapToLong(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();

        BigDecimal avgOrderValue = validOrders.isEmpty() ? BigDecimal.ZERO :
                totalRevenue.divide(BigDecimal.valueOf(validOrders.size()), 2, RoundingMode.HALF_UP);

        // Daily breakdown
        Map<LocalDate, List<Order>> ordersByDate = validOrders.stream()
                .collect(Collectors.groupingBy(o -> 
                        LocalDate.ofInstant(o.getCreatedAt(), ZoneId.systemDefault())));

        List<SalesReportResponse.DailySales> dailySales = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            List<Order> dayOrders = ordersByDate.getOrDefault(current, Collections.emptyList());
            dailySales.add(SalesReportResponse.DailySales.builder()
                    .date(current)
                    .orderCount((long) dayOrders.size())
                    .revenue(calculateRevenue(dayOrders))
                    .itemsSold(dayOrders.stream()
                            .flatMap(o -> o.getItems().stream())
                            .mapToLong(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                            .sum())
                    .build());
            current = current.plusDays(1);
        }

        // Top products
        Map<UUID, Long> productQuantities = new HashMap<>();
        Map<UUID, BigDecimal> productRevenues = new HashMap<>();
        Map<UUID, String> productNames = new HashMap<>();

        validOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .forEach(item -> {
                    UUID productId = item.getProduct().getId();
                    productQuantities.merge(productId, (long) item.getQuantity(), Long::sum);
                    productRevenues.merge(productId, item.getTotalPrice(), BigDecimal::add);
                    productNames.putIfAbsent(productId, item.getProductName());
                });

        List<SalesReportResponse.TopProduct> topProducts = productQuantities.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> SalesReportResponse.TopProduct.builder()
                        .productId(e.getKey().toString())
                        .productName(productNames.get(e.getKey()))
                        .quantitySold(e.getValue())
                        .revenue(productRevenues.get(e.getKey()))
                        .build())
                .collect(Collectors.toList());

        // Order status distribution
        Map<String, Long> statusDistribution = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()));

        return SalesReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalOrders((long) validOrders.size())
                .totalRevenue(totalRevenue)
                .averageOrderValue(avgOrderValue)
                .totalItemsSold(totalItemsSold)
                .dailySales(dailySales)
                .topProducts(topProducts)
                .orderStatusDistribution(statusDistribution)
                .paymentMethodDistribution(new HashMap<>()) // Would need payment method tracking
                .build();
    }

    @Override
    public UserAnalyticsResponse getUserAnalytics(LocalDate startDate, LocalDate endDate) {
        // User counts
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long inactiveUsers = totalUsers - activeUsers;

        // Role distribution
        Map<String, Long> roleDistribution = new HashMap<>();
        roleDistribution.put("ROLE_CUSTOMER", userRepository.findByRoleName("ROLE_CUSTOMER").size() + 0L);
        roleDistribution.put("ROLE_VENDOR", userRepository.findByRoleName("ROLE_VENDOR").size() + 0L);
        roleDistribution.put("ROLE_ADMIN", userRepository.findByRoleName("ROLE_ADMIN").size() + 0L);

        // Top customers by orders
        List<UserAnalyticsResponse.TopCustomer> topByOrders = new ArrayList<>();
        List<UserAnalyticsResponse.TopCustomer> topByRevenue = new ArrayList<>();

        // Would need custom queries for top customers

        return UserAnalyticsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .newUsers(0L) // Would need date-based query
                .registrationTrend(new ArrayList<>())
                .roleDistribution(roleDistribution)
                .topCustomersByOrders(topByOrders)
                .topCustomersByRevenue(topByRevenue)
                .build();
    }

    @Override
    public ProductAnalyticsResponse getProductAnalytics() {
        List<Product> allProducts = productRepository.findAll();
        List<ProductVariant> allVariants = variantRepository.findAll();

        long totalProducts = allProducts.size();
        long activeProducts = allProducts.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).count();
        long inactiveProducts = totalProducts - activeProducts;
        long featuredProducts = allProducts.stream().filter(p -> Boolean.TRUE.equals(p.getIsFeatured())).count();

        // Stock analysis
        Set<UUID> outOfStockProductIds = allVariants.stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() == 0)
                .map(v -> v.getProduct().getId())
                .collect(Collectors.toSet());
        
        Set<UUID> lowStockProductIds = allVariants.stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0 && v.getStockQuantity() <= LOW_STOCK_THRESHOLD)
                .map(v -> v.getProduct().getId())
                .collect(Collectors.toSet());

        long inStockProducts = totalProducts - outOfStockProductIds.size();

        // Category distribution
        Map<String, Long> categoryDistribution = allProducts.stream()
                .filter(p -> p.getCategory() != null)
                .collect(Collectors.groupingBy(p -> p.getCategory().getName(), Collectors.counting()));

        // Brand distribution
        Map<String, Long> brandDistribution = allProducts.stream()
                .filter(p -> p.getBrand() != null)
                .collect(Collectors.groupingBy(p -> p.getBrand().getName(), Collectors.counting()));

        // Top products by various metrics
        List<ProductAnalyticsResponse.ProductPerformance> topByRating = allProducts.stream()
                .filter(p -> p.getAvgRating() != null && p.getAvgRating().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Product::getAvgRating).reversed())
                .limit(10)
                .map(this::mapToPerformance)
                .collect(Collectors.toList());

        List<ProductAnalyticsResponse.ProductPerformance> topByViews = allProducts.stream()
                .filter(p -> p.getViewCount() != null && p.getViewCount() > 0)
                .sorted(Comparator.comparing(Product::getViewCount).reversed())
                .limit(10)
                .map(this::mapToPerformance)
                .collect(Collectors.toList());

        List<ProductAnalyticsResponse.ProductPerformance> topBySold = allProducts.stream()
                .filter(p -> p.getSoldCount() != null && p.getSoldCount() > 0)
                .sorted(Comparator.comparing(Product::getSoldCount).reversed())
                .limit(10)
                .map(this::mapToPerformance)
                .collect(Collectors.toList());

        return ProductAnalyticsResponse.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .inactiveProducts(inactiveProducts)
                .featuredProducts(featuredProducts)
                .inStockProducts(inStockProducts)
                .outOfStockProducts((long) outOfStockProductIds.size())
                .lowStockProducts((long) lowStockProductIds.size())
                .categoryDistribution(categoryDistribution)
                .brandDistribution(brandDistribution)
                .topByRevenue(topBySold) // Using sold count as proxy
                .topByQuantity(topBySold)
                .topByViews(topByViews)
                .topByRating(topByRating)
                .lowPerforming(new ArrayList<>())
                .build();
    }

    @Override
    public InventoryReportResponse getInventoryReport(Integer lowStockThreshold) {
        int threshold = lowStockThreshold != null ? lowStockThreshold : LOW_STOCK_THRESHOLD;
        
        List<ProductVariant> allVariants = variantRepository.findAll();
        
        long totalVariants = allVariants.size();
        long totalProducts = allVariants.stream()
                .map(v -> v.getProduct().getId())
                .distinct()
                .count();

        BigDecimal totalValue = allVariants.stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0)
                .map(v -> {
                    BigDecimal price = v.getFinalPrice(v.getProduct().getBasePrice());
                    return price.multiply(BigDecimal.valueOf(v.getStockQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Stock status counts
        long outOfStock = allVariants.stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() == 0)
                .count();
        
        long lowStock = allVariants.stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0 && v.getStockQuantity() <= threshold)
                .count();
        
        long inStock = totalVariants - outOfStock - lowStock;

        // Out of stock items
        List<InventoryReportResponse.InventoryItem> outOfStockItems = allVariants.stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() == 0)
                .map(this::mapToInventoryItem)
                .collect(Collectors.toList());

        // Low stock items
        List<InventoryReportResponse.InventoryItem> lowStockItems = allVariants.stream()
                .filter(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0 && v.getStockQuantity() <= threshold)
                .sorted(Comparator.comparing(ProductVariant::getStockQuantity))
                .map(this::mapToInventoryItem)
                .collect(Collectors.toList());

        return InventoryReportResponse.builder()
                .totalProducts(totalProducts)
                .totalVariants(totalVariants)
                .totalInventoryValue(totalValue)
                .inStockCount(inStock)
                .outOfStockCount(outOfStock)
                .lowStockCount(lowStock)
                .lowStockThreshold(threshold)
                .outOfStockItems(outOfStockItems)
                .lowStockItems(lowStockItems)
                .overStockItems(new ArrayList<>())
                .build();
    }

    @Override
    public List<RecentActivityResponse> getRecentActivities(int limit) {
        List<RecentActivityResponse> activities = new ArrayList<>();

        // Get recent orders
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        orderRepository.findAll(pageable).forEach(order -> {
            activities.add(RecentActivityResponse.builder()
                    .id(order.getId())
                    .type(RecentActivityResponse.ActivityType.NEW_ORDER)
                    .title("New Order #" + order.getOrderNumber())
                    .description("Order placed for " + order.getTotalAmount())
                    .actorName(order.getUser().getFullName())
                    .actorEmail(order.getUser().getEmail())
                    .actorId(order.getUser().getId())
                    .timestamp(order.getCreatedAt())
                    .build());
        });

        // Sort by timestamp and limit
        return activities.stream()
                .sorted(Comparator.comparing(RecentActivityResponse::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Helper methods

    private BigDecimal calculateRevenue(List<Order> orders) {
        return orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.REFUNDED)
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ProductAnalyticsResponse.ProductPerformance mapToPerformance(Product product) {
        Integer totalStock = product.getVariants().stream()
                .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                .sum();

        return ProductAnalyticsResponse.ProductPerformance.builder()
                .productId(product.getId().toString())
                .productName(product.getName())
                .sku(product.getSku())
                .quantitySold(product.getSoldCount() != null ? product.getSoldCount().longValue() : 0L)
                .revenue(BigDecimal.ZERO) // Would need order data
                .viewCount(product.getViewCount())
                .avgRating(product.getAvgRating())
                .reviewCount(product.getReviewCount())
                .stockQuantity(totalStock)
                .build();
    }

    private InventoryReportResponse.InventoryItem mapToInventoryItem(ProductVariant variant) {
        Product product = variant.getProduct();
        BigDecimal price = variant.getFinalPrice(product.getBasePrice());
        BigDecimal value = variant.getStockQuantity() != null ? 
                price.multiply(BigDecimal.valueOf(variant.getStockQuantity())) : BigDecimal.ZERO;

        return InventoryReportResponse.InventoryItem.builder()
                .productId(product.getId().toString())
                .productName(product.getName())
                .sku(product.getSku())
                .variantId(variant.getId().toString())
                .variantName(variant.getName())
                .stockQuantity(variant.getStockQuantity())
                .reorderLevel(LOW_STOCK_THRESHOLD)
                .unitPrice(price)
                .inventoryValue(value)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .build();
    }
}
