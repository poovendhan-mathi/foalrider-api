package com.foalrider.modules.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for inventory report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReportResponse {

    // Summary
    private Long totalProducts;
    private Long totalVariants;
    private BigDecimal totalInventoryValue;
    
    // Stock status
    private Long inStockCount;
    private Long outOfStockCount;
    private Long lowStockCount;
    private Integer lowStockThreshold;

    // Details
    private List<InventoryItem> outOfStockItems;
    private List<InventoryItem> lowStockItems;
    private List<InventoryItem> overStockItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItem {
        private String productId;
        private String productName;
        private String sku;
        private String variantId;
        private String variantName;
        private Integer stockQuantity;
        private Integer reorderLevel;
        private BigDecimal unitPrice;
        private BigDecimal inventoryValue;
        private String categoryName;
        private String brandName;
    }
}
