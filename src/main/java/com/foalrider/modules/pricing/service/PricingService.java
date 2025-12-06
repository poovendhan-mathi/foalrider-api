package com.foalrider.modules.pricing.service;

import com.foalrider.modules.pricing.dto.*;
import com.foalrider.modules.pricing.entity.Currency;
import com.foalrider.modules.pricing.entity.Region;
import com.foalrider.modules.pricing.entity.RegionalPrice;
import com.foalrider.modules.pricing.entity.ShippingRate;
import com.foalrider.modules.pricing.entity.TaxRate;
import com.foalrider.modules.pricing.repository.*;
import com.foalrider.modules.product.entity.Product;
import com.foalrider.modules.product.repository.ProductRepository;
import com.foalrider.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PricingService {

    private final CurrencyRepository currencyRepository;
    private final RegionRepository regionRepository;
    private final TaxRateRepository taxRateRepository;
    private final ShippingRateRepository shippingRateRepository;
    private final RegionalPriceRepository regionalPriceRepository;
    private final ProductRepository productRepository;

    // =====================
    // Currency Operations
    // =====================

    public List<CurrencyDTO> getAllActiveCurrencies() {
        return currencyRepository.findAllActiveCurrencies().stream()
                .map(this::toDTO)
                .toList();
    }

    public CurrencyDTO getCurrency(String code) {
        return currencyRepository.findByCode(code.toUpperCase())
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "code", code));
    }

    public CurrencyDTO getDefaultCurrency() {
        return currencyRepository.findByIsDefaultTrue()
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Default currency not configured"));
    }

    /**
     * Convert amount from one currency to another via USD base rate
     */
    public BigDecimal convertCurrency(BigDecimal amount, String fromCode, String toCode) {
        if (fromCode.equalsIgnoreCase(toCode)) {
            return amount;
        }

        Currency from = currencyRepository.findByCode(fromCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "code", fromCode));
        Currency to = currencyRepository.findByCode(toCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "code", toCode));

        // Convert to USD first, then to target currency
        BigDecimal usdAmount = amount.divide(from.getExchangeRateToUsd(), 8, RoundingMode.HALF_UP);
        return usdAmount.multiply(to.getExchangeRateToUsd()).setScale(2, RoundingMode.HALF_UP);
    }

    // =====================
    // Region Operations
    // =====================

    public List<RegionDTO> getAllActiveRegions() {
        return regionRepository.findAllActiveRegions().stream()
                .map(this::toDTO)
                .toList();
    }

    public RegionDTO getRegion(String code) {
        return regionRepository.findByCode(code.toUpperCase())
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Region", "code", code));
    }

    public RegionDTO getDefaultRegion() {
        return regionRepository.findByIsDefaultTrue()
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Default region not configured"));
    }

    public RegionDTO getRegionWithFallback(String regionCode) {
        if (regionCode == null || regionCode.isEmpty()) {
            return getDefaultRegion();
        }
        try {
            return getRegion(regionCode);
        } catch (ResourceNotFoundException e) {
            log.warn("Region {} not found, using default", regionCode);
            return getDefaultRegion();
        }
    }

    // =====================
    // Tax Operations
    // =====================

    public List<TaxRateDTO> getTaxRatesForRegion(String regionCode) {
        return taxRateRepository.findActiveByRegionCode(regionCode.toUpperCase()).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Calculate total tax for a given amount in a region
     */
    public TaxCalculationResult calculateTax(BigDecimal amount, String regionCode) {
        List<TaxRate> taxes = taxRateRepository.findActiveByRegionCode(regionCode.toUpperCase());
        
        List<PricingCalculationDTO.TaxLineDTO> taxLines = new ArrayList<>();
        BigDecimal totalTax = BigDecimal.ZERO;
        Currency currency = getRegionCurrency(regionCode);

        for (TaxRate tax : taxes) {
            BigDecimal taxAmount = tax.calculateTax(amount);
            totalTax = totalTax.add(taxAmount);
            
            taxLines.add(PricingCalculationDTO.TaxLineDTO.builder()
                    .name(tax.getName())
                    .displayRate(tax.getDisplayRate())
                    .amount(taxAmount)
                    .formattedAmount(currency.format(taxAmount))
                    .isInclusive(tax.getIsInclusive())
                    .build());
        }

        return new TaxCalculationResult(taxLines, totalTax);
    }

    public record TaxCalculationResult(
            List<PricingCalculationDTO.TaxLineDTO> taxLines,
            BigDecimal totalTax
    ) {}

    // =====================
    // Shipping Operations
    // =====================

    public List<ShippingRateDTO> getShippingRatesForRegion(String regionCode) {
        Currency currency = getRegionCurrency(regionCode);
        return shippingRateRepository.findActiveByRegionCode(regionCode.toUpperCase()).stream()
                .map(rate -> toDTO(rate, currency))
                .toList();
    }

    public ShippingCalculationResult calculateShipping(
            BigDecimal orderTotal, 
            BigDecimal totalWeight, 
            String regionCode, 
            String shippingMethod) {
        
        Currency currency = getRegionCurrency(regionCode);
        ShippingRate rate;

        if (shippingMethod != null && !shippingMethod.isEmpty()) {
            rate = shippingRateRepository.findByRegionCodeAndMethod(regionCode.toUpperCase(), shippingMethod)
                    .orElseGet(() -> getDefaultShippingRate(regionCode));
        } else {
            rate = getDefaultShippingRate(regionCode);
        }

        BigDecimal shippingCost = rate.calculateShippingCost(orderTotal, totalWeight);
        boolean isFreeShipping = shippingCost.compareTo(BigDecimal.ZERO) == 0;
        
        BigDecimal amountToFreeShipping = BigDecimal.ZERO;
        if (!isFreeShipping && rate.getFreeShippingThreshold() != null) {
            amountToFreeShipping = rate.getFreeShippingThreshold().subtract(orderTotal).max(BigDecimal.ZERO);
        }

        return new ShippingCalculationResult(
                rate.getShippingMethod(),
                rate.getName(),
                shippingCost,
                currency.format(shippingCost),
                rate.getDeliveryEstimate(),
                isFreeShipping,
                amountToFreeShipping,
                currency.format(amountToFreeShipping)
        );
    }

    private ShippingRate getDefaultShippingRate(String regionCode) {
        return shippingRateRepository.findDefaultByRegionCode(regionCode.toUpperCase())
                .orElseGet(() -> shippingRateRepository.findActiveByRegionCode(regionCode.toUpperCase())
                        .stream().findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("No shipping rates for region: " + regionCode)));
    }

    public record ShippingCalculationResult(
            String method,
            String name,
            BigDecimal cost,
            String formattedCost,
            String deliveryEstimate,
            boolean isFreeShipping,
            BigDecimal amountToFreeShipping,
            String formattedAmountToFreeShipping
    ) {}

    // =====================
    // Product Pricing
    // =====================

    /**
     * Get price for a product in a specific region
     * Falls back to product's base price if no regional price exists
     */
    public RegionalPriceDTO getProductPrice(UUID productId, String regionCode) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        Region region = regionRepository.findByCode(regionCode.toUpperCase())
                .orElseGet(() -> regionRepository.findByIsDefaultTrue()
                        .orElseThrow(() -> new ResourceNotFoundException("Default region not configured")));

        Optional<RegionalPrice> regionalPrice = regionalPriceRepository.findByProductIdAndRegionCode(productId, region.getCode());

        if (regionalPrice.isPresent()) {
            return toDTO(regionalPrice.get());
        }

        // Fallback to product's base price, converted to region's currency
        Currency currency = region.getDefaultCurrency();
        BigDecimal basePrice = product.getBasePrice();
        BigDecimal salePrice = product.getSalePrice();
        BigDecimal finalPrice = (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0) 
                ? salePrice : basePrice;

        return RegionalPriceDTO.builder()
                .productId(productId)
                .productName(product.getName())
                .regionCode(region.getCode())
                .currencyCode(currency.getCode())
                .basePrice(basePrice)
                .salePrice(salePrice)
                .finalPrice(finalPrice)
                .discountPercentage(calculateDiscount(basePrice, salePrice))
                .formattedBasePrice(currency.format(basePrice))
                .formattedSalePrice(salePrice != null ? currency.format(salePrice) : null)
                .formattedFinalPrice(currency.format(finalPrice))
                .isActive(true)
                .build();
    }

    /**
     * Get prices for multiple products in a region (batch)
     */
    public Map<UUID, RegionalPriceDTO> getProductPrices(List<UUID> productIds, String regionCode) {
        Map<UUID, RegionalPriceDTO> priceMap = new HashMap<>();
        
        // Get regional prices
        List<RegionalPrice> regionalPrices = regionalPriceRepository.findByProductIdsAndRegionCode(productIds, regionCode.toUpperCase());
        Set<UUID> foundProductIds = new HashSet<>();
        
        for (RegionalPrice rp : regionalPrices) {
            priceMap.put(rp.getProduct().getId(), toDTO(rp));
            foundProductIds.add(rp.getProduct().getId());
        }

        // Get fallback prices for products without regional pricing
        List<UUID> missingIds = productIds.stream()
                .filter(id -> !foundProductIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            for (UUID productId : missingIds) {
                try {
                    priceMap.put(productId, getProductPrice(productId, regionCode));
                } catch (ResourceNotFoundException e) {
                    log.warn("Product {} not found when getting prices", productId);
                }
            }
        }

        return priceMap;
    }

    // =====================
    // Full Cart/Order Pricing
    // =====================

    /**
     * Calculate complete pricing for a cart/order
     */
    public PricingCalculationDTO calculateFullPricing(
            List<CartItem> items,
            String regionCode,
            String shippingMethod,
            String overrideCurrency) {
        
        Region region = regionRepository.findByCode(regionCode.toUpperCase())
                .orElseGet(() -> regionRepository.findByIsDefaultTrue()
                        .orElseThrow(() -> new ResourceNotFoundException("Default region not configured")));

        Currency currency = (overrideCurrency != null && !overrideCurrency.isEmpty())
                ? currencyRepository.findByCode(overrideCurrency.toUpperCase())
                        .orElse(region.getDefaultCurrency())
                : region.getDefaultCurrency();

        // Calculate line items
        List<PricingCalculationDTO.PricedItemDTO> pricedItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : items) {
            RegionalPriceDTO price = getProductPrice(item.productId(), regionCode);
            BigDecimal unitPrice = price.getFinalPrice();
            
            // Convert if using different currency than regional price
            if (!currency.getCode().equals(price.getCurrencyCode())) {
                unitPrice = convertCurrency(unitPrice, price.getCurrencyCode(), currency.getCode());
            }
            
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
            subtotal = subtotal.add(lineTotal);

            pricedItems.add(PricingCalculationDTO.PricedItemDTO.builder()
                    .productId(item.productId().toString())
                    .productName(price.getProductName())
                    .quantity(item.quantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .formattedUnitPrice(currency.format(unitPrice))
                    .formattedLineTotal(currency.format(lineTotal))
                    .build());
        }

        // Calculate tax
        TaxCalculationResult taxResult = calculateTax(subtotal, regionCode);

        // Calculate shipping
        ShippingCalculationResult shippingResult = calculateShipping(
                subtotal, BigDecimal.ZERO, regionCode, shippingMethod);

        // Calculate grand total
        BigDecimal grandTotal = subtotal.add(taxResult.totalTax()).add(shippingResult.cost());

        return PricingCalculationDTO.builder()
                .regionCode(region.getCode())
                .regionName(region.getName())
                .currencyCode(currency.getCode())
                .currencySymbol(currency.getSymbol())
                .items(pricedItems)
                .subtotal(subtotal)
                .formattedSubtotal(currency.format(subtotal))
                .taxes(taxResult.taxLines())
                .totalTax(taxResult.totalTax())
                .formattedTotalTax(currency.format(taxResult.totalTax()))
                .shippingMethod(shippingResult.method())
                .shippingCost(shippingResult.cost())
                .formattedShippingCost(shippingResult.formattedCost())
                .deliveryEstimate(shippingResult.deliveryEstimate())
                .qualifiesForFreeShipping(shippingResult.isFreeShipping())
                .amountToFreeShipping(shippingResult.amountToFreeShipping())
                .grandTotal(grandTotal)
                .formattedGrandTotal(currency.format(grandTotal))
                .exchangeRate(currency.getExchangeRateToUsd())
                .baseCurrencyCode("USD")
                .build();
    }

    public record CartItem(UUID productId, int quantity) {}

    // =====================
    // Stripe Integration
    // =====================

    /**
     * Get Stripe-compatible amount for a given price
     */
    public long getStripeAmount(BigDecimal amount, String currencyCode) {
        Currency currency = currencyRepository.findByCode(currencyCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "code", currencyCode));
        return currency.toStripeAmount(amount);
    }

    /**
     * Check if Stripe supports the currency
     */
    public boolean isStripeSupportedCurrency(String currencyCode) {
        // Major currencies supported by Stripe
        Set<String> supported = Set.of(
                "USD", "EUR", "GBP", "SGD", "INR", "AUD", "CAD", "JPY", "CNY", 
                "HKD", "NZD", "CHF", "SEK", "NOK", "DKK", "MXN", "BRL", "MYR"
        );
        return supported.contains(currencyCode.toUpperCase());
    }

    // =====================
    // Helper Methods
    // =====================

    private Currency getRegionCurrency(String regionCode) {
        Region region = regionRepository.findByCode(regionCode.toUpperCase())
                .orElseGet(() -> regionRepository.findByIsDefaultTrue()
                        .orElseThrow(() -> new ResourceNotFoundException("Default region not configured")));
        return region.getDefaultCurrency();
    }

    private Integer calculateDiscount(BigDecimal basePrice, BigDecimal salePrice) {
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0 
                || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return basePrice.subtract(salePrice)
                .divide(basePrice, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .intValue();
    }

    // =====================
    // DTO Mappers
    // =====================

    private CurrencyDTO toDTO(Currency currency) {
        return CurrencyDTO.builder()
                .id(currency.getId())
                .code(currency.getCode())
                .name(currency.getName())
                .symbol(currency.getSymbol())
                .symbolPosition(currency.getSymbolPosition())
                .decimalPlaces(currency.getDecimalPlaces())
                .decimalSeparator(currency.getDecimalSeparator())
                .thousandsSeparator(currency.getThousandsSeparator())
                .exchangeRateToUsd(currency.getExchangeRateToUsd())
                .isActive(currency.getIsActive())
                .isDefault(currency.getIsDefault())
                .build();
    }

    private RegionDTO toDTO(Region region) {
        return RegionDTO.builder()
                .id(region.getId())
                .code(region.getCode())
                .name(region.getName())
                .timezone(region.getTimezone())
                .localeCode(region.getLocaleCode())
                .dateFormat(region.getDateFormat())
                .isActive(region.getIsActive())
                .isDefault(region.getIsDefault())
                .defaultCurrency(toDTO(region.getDefaultCurrency()))
                .build();
    }

    private TaxRateDTO toDTO(TaxRate taxRate) {
        return TaxRateDTO.builder()
                .id(taxRate.getId())
                .regionCode(taxRate.getRegion().getCode())
                .name(taxRate.getName())
                .description(taxRate.getDescription())
                .rate(taxRate.getRate())
                .displayRate(taxRate.getDisplayRate())
                .isInclusive(taxRate.getIsInclusive())
                .isActive(taxRate.getIsActive())
                .priority(taxRate.getPriority())
                .build();
    }

    private ShippingRateDTO toDTO(ShippingRate rate, Currency currency) {
        return ShippingRateDTO.builder()
                .id(rate.getId())
                .regionCode(rate.getRegion().getCode())
                .shippingMethod(rate.getShippingMethod())
                .name(rate.getName())
                .description(rate.getDescription())
                .baseCost(rate.getBaseCost())
                .costPerKg(rate.getCostPerKg())
                .freeShippingThreshold(rate.getFreeShippingThreshold())
                .minDeliveryDays(rate.getMinDeliveryDays())
                .maxDeliveryDays(rate.getMaxDeliveryDays())
                .deliveryEstimate(rate.getDeliveryEstimate())
                .isActive(rate.getIsActive())
                .isDefault(rate.getIsDefault())
                .formattedBaseCost(currency.format(rate.getBaseCost()))
                .formattedFreeThreshold(rate.getFreeShippingThreshold() != null 
                        ? currency.format(rate.getFreeShippingThreshold()) : null)
                .build();
    }

    private RegionalPriceDTO toDTO(RegionalPrice rp) {
        Currency currency = rp.getCurrency();
        return RegionalPriceDTO.builder()
                .id(rp.getId())
                .productId(rp.getProduct().getId())
                .productName(rp.getProduct().getName())
                .regionCode(rp.getRegion().getCode())
                .currencyCode(currency.getCode())
                .basePrice(rp.getBasePrice())
                .salePrice(rp.getSalePrice())
                .finalPrice(rp.getFinalPrice())
                .discountPercentage(rp.getDiscountPercentage())
                .formattedBasePrice(rp.getFormattedBasePrice())
                .formattedSalePrice(rp.getSalePrice() != null ? currency.format(rp.getSalePrice()) : null)
                .formattedFinalPrice(rp.getFormattedPrice())
                .isActive(rp.getIsActive())
                .build();
    }
}
