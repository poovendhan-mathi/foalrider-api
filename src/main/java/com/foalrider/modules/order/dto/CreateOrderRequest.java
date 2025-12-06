package com.foalrider.modules.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "Shipping address is required")
    @Valid
    private ShippingAddressRequest shippingAddress;

    private BillingAddressRequest billingAddress;

    private String customerNotes;

    private String couponCode;
}
