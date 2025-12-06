package com.foalrider.modules.pricing.repository;

import com.foalrider.modules.pricing.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, UUID> {

    Optional<Currency> findByCode(String code);

    Optional<Currency> findByIsDefaultTrue();

    List<Currency> findByIsActiveTrue();

    @Query("SELECT c FROM Currency c WHERE c.isActive = true ORDER BY c.isDefault DESC, c.code ASC")
    List<Currency> findAllActiveCurrencies();

    boolean existsByCode(String code);
}
