package com.foalrider.modules.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Product image response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponse {
    private UUID id;
    private String url;
    private String altText;
    private Integer displayOrder;
    private Boolean isPrimary;
}
