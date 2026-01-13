package com.foalrider.modules.user.repository;

import com.foalrider.modules.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Address entity.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    /**
     * Find all addresses for a user.
     */
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    /**
     * Find address by ID and user ID.
     */
    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find default address for a user.
     */
    Optional<Address> findByUserIdAndIsDefaultTrue(UUID userId);

    /**
     * Count addresses for a user.
     */
    int countByUserId(UUID userId);

    /**
     * Reset default address for a user.
     */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND a.isDefault = true")
    void resetDefaultAddress(@Param("userId") UUID userId);

    /**
     * Delete address by ID and user ID.
     */
    void deleteByIdAndUserId(UUID id, UUID userId);
}
