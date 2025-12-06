package com.foalrider.modules.order.repository;

import com.foalrider.modules.order.entity.Order;
import com.foalrider.modules.order.entity.OrderStatus;
import com.foalrider.modules.order.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByDateRange(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

    boolean existsByOrderNumber(String orderNumber);

    Optional<Order> findByPaymentIntentId(String paymentIntentId);
}
