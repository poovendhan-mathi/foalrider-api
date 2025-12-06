package com.foalrider.modules.pricing.controller;

import com.foalrider.modules.pricing.dto.*;
import com.foalrider.modules.pricing.service.PricingService;
import com.foalrider.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing", description = "Multi-currency and regional pricing APIs")
public class PricingController {

    private final PricingService pricingService;

    // =====================
    // Currency Endpoints
    // =====================

    @GetMapping("/currencies")
    @Operation(summary = "Get all active currencies")
    public ResponseEntity<ApiResponse<List<CurrencyDTO>>> getAllCurrencies() {
        List<CurrencyDTO> currencies = pricingService.getAllActiveCurrencies();
        return ResponseEntity.ok(ApiResponse.success(currencies, "Currencies retrieved"));
    }

    @GetMapping("/currencies/{code}")
    @Operation(summary = "Get currency by code")
    public ResponseEntity<ApiResponse<CurrencyDTO>> getCurrency(
            @PathVariable String code) {
        CurrencyDTO currency = pricingService.getCurrency(code);
        return ResponseEntity.ok(ApiResponse.success(currency, "Currency retrieved"));
    }

    @GetMapping("/currencies/default")
    @Operation(summary = "Get default currency")
    public ResponseEntity<ApiResponse<CurrencyDTO>> getDefaultCurrency() {
        CurrencyDTO currency = pricingService.getDefaultCurrency();
        return ResponseEntity.ok(ApiResponse.success(currency, "Default currency retrieved"));
    }

    @GetMapping("/convert")
    @Operation(summary = "Convert amount between currencies")
    public ResponseEntity<ApiResponse<CurrencyConversionResponse>> convertCurrency(
            @RequestParam BigDecimal amount,
            @RequestParam String from,
            @RequestParam String to) {
        BigDecimal converted = pricingService.convertCurrency(amount, from, to);
        
        CurrencyConversionResponse response = CurrencyConversionResponse.builder()
                .originalAmount(amount)
                .originalCurrency(from.toUpperCase())
                .convertedAmount(converted)
                .targetCurrency(to.toUpperCase())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(response, "Conversion successful"));
    }

    // =====================
    // Region Endpoints
    // =====================

    @GetMapping("/regions")
    @Operation(summary = "Get all active regions")
    public ResponseEntity<ApiResponse<List<RegionDTO>>> getAllRegions() {
        List<RegionDTO> regions = pricingService.getAllActiveRegions();
        return ResponseEntity.ok(ApiResponse.success(regions, "Regions retrieved"));
    }

    @GetMapping("/regions/{code}")
    @Operation(summary = "Get region by code")
    public ResponseEntity<ApiResponse<RegionDTO>> getRegion(
            @PathVariable String code) {
        RegionDTO region = pricingService.getRegion(code);
        return ResponseEntity.ok(ApiResponse.success(region, "Region retrieved"));
    }

    @GetMapping("/regions/default")
    @Operation(summary = "Get default region")
    public ResponseEntity<ApiResponse<RegionDTO>> getDefaultRegion() {
        RegionDTO region = pricingService.getDefaultRegion();
        return ResponseEntity.ok(ApiResponse.success(region, "Default region retrieved"));
    }

    // =====================
    // Tax Endpoints
    // =====================

    @GetMapping("/regions/{regionCode}/taxes")
    @Operation(summary = "Get tax rates for a region")
    public ResponseEntity<ApiResponse<List<TaxRateDTO>>> getTaxRates(
            @PathVariable String regionCode) {
        List<TaxRateDTO> taxes = pricingService.getTaxRatesForRegion(regionCode);
        return ResponseEntity.ok(ApiResponse.success(taxes, "Tax rates retrieved"));
    }

    @GetMapping("/calculate-tax")
    @Operation(summary = "Calculate tax for an amount in a region")
    public ResponseEntity<ApiResponse<TaxCalculationResponse>> calculateTax(
            @RequestParam BigDecimal amount,
            @RequestParam String regionCode) {
        PricingService.TaxCalculationResult result = pricingService.calculateTax(amount, regionCode);
        
        TaxCalculationResponse response = TaxCalculationResponse.builder()
                .baseAmount(amount)
                .regionCode(regionCode)
                .taxes(result.taxLines())
                .totalTax(result.totalTax())
                .totalWithTax(amount.add(result.totalTax()))
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(response, "Tax calculated"));
    }

    // =====================
    // Shipping Endpoints
    // =====================

    @GetMapping("/regions/{regionCode}/shipping")
    @Operation(summary = "Get shipping rates for a region")
    public ResponseEntity<ApiResponse<List<ShippingRateDTO>>> getShippingRates(
            @PathVariable String regionCode) {
        List<ShippingRateDTO> rates = pricingService.getShippingRatesForRegion(regionCode);
        return ResponseEntity.ok(ApiResponse.success(rates, "Shipping rates retrieved"));
    }

    @GetMapping("/calculate-shipping")
    @Operation(summary = "Calculate shipping cost")
    public ResponseEntity<ApiResponse<ShippingCalculationResponse>> calculateShipping(
            @RequestParam BigDecimal orderTotal,
            @RequestParam(defaultValue = "0") BigDecimal weight,
            @RequestParam String regionCode,
            @RequestParam(required = false) String shippingMethod) {
        
        PricingService.ShippingCalculationResult result = pricingService.calculateShipping(
                orderTotal, weight, regionCode, shippingMethod);
        
        ShippingCalculationResponse response = ShippingCalculationResponse.builder()
                .method(result.method())
                .name(result.name())
                .cost(result.cost())
                .formattedCost(result.formattedCost())
                .deliveryEstimate(result.deliveryEstimate())
                .isFreeShipping(result.isFreeShipping())
                .amountToFreeShipping(result.amountToFreeShipping())
                .formattedAmountToFreeShipping(result.formattedAmountToFreeShipping())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(response, "Shipping calculated"));
    }

    // =====================
    // Product Pricing
    // =====================

    @GetMapping("/products/{productId}/price")
    @Operation(summary = "Get product price for a region")
    public ResponseEntity<ApiResponse<RegionalPriceDTO>> getProductPrice(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "US") 
            @Parameter(description = "Region code (US, SG, IN, GB)") String region) {
        RegionalPriceDTO price = pricingService.getProductPrice(productId, region);
        return ResponseEntity.ok(ApiResponse.success(price, "Product price retrieved"));
    }

    @PostMapping("/products/prices")
    @Operation(summary = "Get prices for multiple products in a region (batch)")
    public ResponseEntity<ApiResponse<Map<UUID, RegionalPriceDTO>>> getProductPrices(
            @RequestBody List<UUID> productIds,
            @RequestParam(defaultValue = "US") String region) {
        Map<UUID, RegionalPriceDTO> prices = pricingService.getProductPrices(productIds, region);
        return ResponseEntity.ok(ApiResponse.success(prices, "Product prices retrieved"));
    }

    // =====================
    // Full Pricing Calculation
    // =====================

    @PostMapping("/calculate")
    @Operation(summary = "Calculate full pricing for cart items")
    public ResponseEntity<ApiResponse<PricingCalculationDTO>> calculateFullPricing(
            @RequestBody PricingCalculationRequest request) {
        
        List<PricingService.CartItem> items = request.getItems().stream()
                .map(i -> new PricingService.CartItem(i.getProductId(), i.getQuantity()))
                .toList();
        
        PricingCalculationDTO pricing = pricingService.calculateFullPricing(
                items,
                request.getRegionCode(),
                request.getShippingMethod(),
                request.getCurrencyCode());
        
        return ResponseEntity.ok(ApiResponse.success(pricing, "Pricing calculated"));
    }

    // =====================
    // Response DTOs
    // =====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CurrencyConversionResponse {
        private BigDecimal originalAmount;
        private String originalCurrency;
        private BigDecimal convertedAmount;
        private String targetCurrency;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TaxCalculationResponse {
        private BigDecimal baseAmount;
        private String regionCode;
        private List<PricingCalculationDTO.TaxLineDTO> taxes;
        private BigDecimal totalTax;
        private BigDecimal totalWithTax;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ShippingCalculationResponse {
        private String method;
        private String name;
        private BigDecimal cost;
        private String formattedCost;
        private String deliveryEstimate;
        private boolean isFreeShipping;
        private BigDecimal amountToFreeShipping;
        private String formattedAmountToFreeShipping;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PricingCalculationRequest {
        private List<CartItemRequest> items;
        private String regionCode;
        private String shippingMethod;
        private String currencyCode; // Optional override
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CartItemRequest {
        private UUID productId;
        private int quantity;
    }
}
