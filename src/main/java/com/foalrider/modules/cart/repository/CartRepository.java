package com.foalrider.modules.cart.repository;

import com.foalrider.modules.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") UUID userId);

    Optional<Cart> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
