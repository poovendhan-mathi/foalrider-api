package com.foalrider.modules.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private String productSlug;
    private String productImage;
    private UUID variantId;
    private String variantName;
    private String variantSku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Integer availableStock;
    private Boolean inStock;
}
