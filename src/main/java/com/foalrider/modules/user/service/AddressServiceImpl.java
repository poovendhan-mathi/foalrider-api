package com.foalrider.modules.user.service;

import com.foalrider.modules.user.dto.AddressRequest;
import com.foalrider.modules.user.dto.AddressResponse;
import com.foalrider.modules.user.entity.Address;
import com.foalrider.modules.user.entity.User;
import com.foalrider.modules.user.repository.AddressRepository;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.security.SecurityUtils;
import com.foalrider.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of AddressService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    private static final int MAX_ADDRESSES_PER_USER = 10;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getCurrentUserAddresses() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        List<Address> addresses = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        return addresses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(UUID addressId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        return mapToResponse(address);
    }

    @Override
    public AddressResponse addAddress(AddressRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check address limit
        int currentCount = addressRepository.countByUserId(userId);
        if (currentCount >= MAX_ADDRESSES_PER_USER) {
            throw new IllegalStateException("Maximum number of addresses (" + MAX_ADDRESSES_PER_USER + ") reached");
        }

        // If this is the first address or marked as default, set it as default
        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault()) || currentCount == 0;

        if (isDefault) {
            addressRepository.resetDefaultAddress(userId);
        }

        Address address = Address.builder()
                .user(user)
                .label(request.getLabel())
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .countryCode(request.getCountryCode())
                .isDefault(isDefault)
                .isBillingAddress(Boolean.TRUE.equals(request.getIsBillingAddress()))
                .build();

        address = addressRepository.save(address);
        log.info("Address added for user {}: {}", userId, address.getId());

        return mapToResponse(address);
    }

    @Override
    public AddressResponse updateAddress(UUID addressId, AddressRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Handle default address change
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.resetDefaultAddress(userId);
        }

        address.setLabel(request.getLabel());
        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setCountryCode(request.getCountryCode());
        
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }
        if (request.getIsBillingAddress() != null) {
            address.setIsBillingAddress(request.getIsBillingAddress());
        }

        address = addressRepository.save(address);
        log.info("Address updated: {}", addressId);

        return mapToResponse(address);
    }

    @Override
    public void deleteAddress(UUID addressId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);

        // If deleted address was default, set another address as default
        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        }

        log.info("Address deleted: {}", addressId);
    }

    @Override
    public AddressResponse setDefaultAddress(UUID addressId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Reset all defaults and set this one
        addressRepository.resetDefaultAddress(userId);
        address.setIsDefault(true);
        address = addressRepository.save(address);

        log.info("Default address set: {}", addressId);
        return mapToResponse(address);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        Address address = addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No default address found"));
        return mapToResponse(address);
    }

    /**
     * Map Address entity to AddressResponse DTO.
     */
    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .countryCode(address.getCountryCode())
                .isDefault(address.getIsDefault())
                .isBillingAddress(address.getIsBillingAddress())
                .fullAddress(address.getFullAddress())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
