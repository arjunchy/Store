package com.ecommerce.backend.service.address;

import com.ecommerce.backend.dto.request.AddressRequest;
import com.ecommerce.backend.dto.response.AddressResponse;
import com.ecommerce.backend.entity.Address;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.repository.AddressRepository;
import com.ecommerce.backend.repository.UserRepository;
import com.ecommerce.backend.mapper.AddressMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressMapper addressMapper;

    @Transactional
    public AddressResponse create(AddressRequest request, String userId) {
        log.info("Creating address for userId: {} with label: {}", userId, request.label());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

        if (Boolean.TRUE.equals(request.isDefault())) {
            log.debug("isDefault=true, unsetting other default addresses for userId: {}", userId);
            unsetOtherDefaults(userId);
        }

        Address address = Address.builder()
                .user(user)
                .label(request.label())
                .street(request.street())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country())
                .isDefault(Boolean.TRUE.equals(request.isDefault()))
                .build();

        Address saved = addressRepository.save(address);
        log.info("Successfully created address with id: {} for userId: {}", saved.getId(), userId);
        return addressMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAll(String userId) {
        log.debug("Fetching all addresses for userId: {}", userId);
        List<Address> addresses = addressRepository.findByUserId(userId);
        log.info("Retrieved {} addresses for userId: {}", addresses.size(), userId);
        return addresses.stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AddressResponse getById(String id, String userId) {
        log.debug("Fetching address by id: {} for userId: {}", id, userId);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Address not found with id: {}", id);
                    return new IllegalArgumentException("Address not found");
                });

        if (!address.getUser().getUserId().equals(userId)) {
            log.warn("Access denied - address {} does not belong to user {}", id, userId);
            throw new IllegalArgumentException("Address not found");
        }

        log.debug("Found address with id: {} for userId: {}", id, userId);
        return addressMapper.toResponse(address);
    }

    @Transactional
    public AddressResponse update(String id, AddressRequest request, String userId) {
        log.info("Updating address with id: {} for userId: {}", id, userId);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Address not found with id: {}", id);
                    return new IllegalArgumentException("Address not found");
                });

        if (!address.getUser().getUserId().equals(userId)) {
            log.warn("Access denied - address {} does not belong to user {}", id, userId);
            throw new IllegalArgumentException("Address not found");
        }

        if (Boolean.TRUE.equals(request.isDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            log.debug("isDefault set to true, unsetting other defaults for userId: {}", userId);
            unsetOtherDefaults(userId);
        }

        address.setLabel(request.label());
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country());
        address.setIsDefault(Boolean.TRUE.equals(request.isDefault()));

        Address saved = addressRepository.save(address);
        log.info("Successfully updated address with id: {} for userId: {}", saved.getId(), userId);
        return addressMapper.toResponse(saved);
    }

    /**
     * Partial update: only fields present in the request body are changed.
     * Required fields (street/city/postalCode/country) may not be removed.
     */
    @Transactional
    public AddressResponse updatePartial(String id, Map<String, Object> body, String userId) {
        log.info("Partially updating address with id: {} for userId: {}", id, userId);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Address not found with id: {}", id);
                    return new IllegalArgumentException("Address not found");
                });

        if (!address.getUser().getUserId().equals(userId)) {
            log.warn("Access denied - address {} does not belong to user {}", id, userId);
            throw new IllegalArgumentException("Address not found");
        }

        if (body.containsKey("isDefault")
                && Boolean.TRUE.equals(boolVal(body.get("isDefault")))
                && !Boolean.TRUE.equals(address.getIsDefault())) {
            log.debug("isDefault set to true, unsetting other defaults for userId: {}", userId);
            unsetOtherDefaults(userId);
        }

        if (body.containsKey("label")) address.setLabel(strVal(body.get("label")));
        if (body.containsKey("street")) address.setStreet(strVal(body.get("street")));
        if (body.containsKey("city")) address.setCity(strVal(body.get("city")));
        if (body.containsKey("state")) address.setState(strVal(body.get("state")));
        if (body.containsKey("postalCode")) address.setPostalCode(strVal(body.get("postalCode")));
        if (body.containsKey("country")) address.setCountry(strVal(body.get("country")));
        if (body.containsKey("isDefault")) address.setIsDefault(Boolean.TRUE.equals(boolVal(body.get("isDefault"))));

        List<String> missing = new ArrayList<>();
        if (address.getStreet() == null || address.getStreet().isBlank()) missing.add("street");
        if (address.getCity() == null || address.getCity().isBlank()) missing.add("city");
        if (address.getPostalCode() == null || address.getPostalCode().isBlank()) missing.add("postalCode");
        if (address.getCountry() == null || address.getCountry().isBlank()) missing.add("country");
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Cannot remove required address fields: " + String.join(", ", missing));
        }

        Address saved = addressRepository.save(address);
        log.info("Successfully updated address with id: {} for userId: {}", saved.getId(), userId);
        return addressMapper.toResponse(saved);
    }

    @Transactional
    public void delete(String id, String userId) {
        log.info("Deleting address with id: {} for userId: {}", id, userId);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Address not found with id: {}", id);
                    return new IllegalArgumentException("Address not found");
                });

        if (!address.getUser().getUserId().equals(userId)) {
            log.warn("Access denied - address {} does not belong to user {}", id, userId);
            throw new IllegalArgumentException("Address not found");
        }

        addressRepository.delete(address);
        log.info("Successfully deleted address with id: {} for userId: {}", id, userId);
    }
    private void unsetOtherDefaults(String userId) {
        try {
            addressRepository.unsetOtherDefaults(userId);
        } catch (Exception e) {
            log.error("Failed to unset other default addresses for userId: {}", userId, e);
            throw e;
        }
    }

    private String strVal(Object val) {
        return val == null ? null : String.valueOf(val).trim();
    }

    private Boolean boolVal(Object val) {
        if (val == null) return null;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(String.valueOf(val));
    }

}
