package com.foalrider.modules.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents calculated pricing for a cart/order in a specific region
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingCalculationDTO {

    // Region & Currency info
    private String regionCode;
    private String regionName;
    private String currencyCode;
    private String currencySymbol;

    // Line items
    private List<PricedItemDTO> items;

    // Subtotals
    private BigDecimal subtotal;          // Before tax & shipping
    private String formattedSubtotal;

    // Taxes
    private List<TaxLineDTO> taxes;
    private BigDecimal totalTax;
    private String formattedTotalTax;

    // Shipping
    private String shippingMethod;
    private BigDecimal shippingCost;
    private String formattedShippingCost;
    private String deliveryEstimate;
    private Boolean qualifiesForFreeShipping;
    private BigDecimal amountToFreeShipping;  // How much more to spend for free shipping

    // Grand Total
    private BigDecimal grandTotal;
    private String formattedGrandTotal;

    // Conversion info (if currency converted)
    private BigDecimal exchangeRate;
    private String baseCurrencyCode;       // Original currency if converted

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricedItemDTO {
        private String productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private String formattedUnitPrice;
        private String formattedLineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxLineDTO {
        private String name;           // GST, VAT, Sales Tax
        private String displayRate;    // "7%", "18%"
        private BigDecimal amount;
        private String formattedAmount;
        private Boolean isInclusive;
    }
}
