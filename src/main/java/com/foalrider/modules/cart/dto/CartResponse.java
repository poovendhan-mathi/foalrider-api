package com.foalrider.modules.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private UUID id;
    private UUID userId;
    private List<CartItemResponse> items;
    private Integer totalItems;
    
    // Regional pricing info
    private String regionCode;
    private String regionName;
    private String currencyCode;
    private String currencySymbol;
    
    // Amounts
    private BigDecimal subtotal;
    private String formattedSubtotal;
    
    private BigDecimal tax;
    private String formattedTax;
    private String taxName; // GST, VAT, Sales Tax
    private String taxRate; // "8%", "18%"
    
    private BigDecimal shipping;
    private String formattedShipping;
    private String shippingMethod;
    private String deliveryEstimate;
    private Boolean qualifiesForFreeShipping;
    private BigDecimal amountToFreeShipping;
    
    private BigDecimal total;
    private String formattedTotal;
}
