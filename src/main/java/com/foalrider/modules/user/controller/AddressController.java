package com.foalrider.modules.user.controller;

import com.foalrider.modules.user.dto.AddressRequest;
import com.foalrider.modules.user.dto.AddressResponse;
import com.foalrider.modules.user.service.AddressService;
import com.foalrider.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user address endpoints.
 */
@RestController
@RequestMapping("/users/me/addresses")
@RequiredArgsConstructor
@Tag(name = "User Addresses", description = "User address management endpoints")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "Get all addresses", description = "Get all addresses for the current user")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses() {
        List<AddressResponse> addresses = addressService.getCurrentUserAddresses();
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by ID", description = "Get a specific address by ID")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddress(@PathVariable UUID id) {
        AddressResponse address = addressService.getAddressById(id);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    @GetMapping("/default")
    @Operation(summary = "Get default address", description = "Get the default address for the current user")
    public ResponseEntity<ApiResponse<AddressResponse>> getDefaultAddress() {
        AddressResponse address = addressService.getDefaultAddress();
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    @PostMapping
    @Operation(summary = "Add address", description = "Add a new address for the current user")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @Valid @RequestBody AddressRequest request) {
        AddressResponse address = addressService.addAddress(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(address, "Address added successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update address", description = "Update an existing address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse address = addressService.updateAddress(id, request);
        return ResponseEntity.ok(ApiResponse.success(address, "Address updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address", description = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable UUID id) {
        addressService.deleteAddress(id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully"));
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "Set default address", description = "Set an address as the default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(@PathVariable UUID id) {
        AddressResponse address = addressService.setDefaultAddress(id);
        return ResponseEntity.ok(ApiResponse.success(address, "Default address updated"));
    }
}
