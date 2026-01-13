package com.foalrider.modules.user.service;

import com.foalrider.modules.user.dto.AddressRequest;
import com.foalrider.modules.user.dto.AddressResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for address operations.
 */
public interface AddressService {

    /**
     * Get all addresses for the current user.
     */
    List<AddressResponse> getCurrentUserAddresses();

    /**
     * Get address by ID for current user.
     */
    AddressResponse getAddressById(UUID addressId);

    /**
     * Add a new address for current user.
     */
    AddressResponse addAddress(AddressRequest request);

    /**
     * Update an existing address.
     */
    AddressResponse updateAddress(UUID addressId, AddressRequest request);

    /**
     * Delete an address.
     */
    void deleteAddress(UUID addressId);

    /**
     * Set address as default.
     */
    AddressResponse setDefaultAddress(UUID addressId);

    /**
     * Get default address for current user.
     */
    AddressResponse getDefaultAddress();
}
