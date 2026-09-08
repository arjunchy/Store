package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.AddressRequest;
import com.ecommerce.backend.dto.response.AddressResponse;
import com.ecommerce.backend.service.address.AddressService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@Slf4j
public class AddressController {

    @Autowired
    private AddressService addressService;

    @PostMapping
    public ResponseEntity<AddressResponse> create(
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "POST /api/addresses - Creating address for userId: {}",
                userId
        );

        try {
            AddressResponse created =
                    addressService.create(request, userId);

            log.info(
                    "POST /api/addresses - Successfully created address with id: {} for userId: {}",
                    created.id(),
                    userId
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(created);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "POST /api/addresses - Create failed - {}",
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "POST /api/addresses - Unexpected error for userId: {}",
                    userId,
                    e
            );

            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "GET /api/addresses - Fetching all addresses for userId: {}",
                userId
        );

        try {
            List<AddressResponse> addresses =
                    addressService.getAll(userId);

            log.debug(
                    "GET /api/addresses - Retrieved {} addresses for userId: {}",
                    addresses.size(),
                    userId
            );

            return ResponseEntity.ok(addresses);

        } catch (Exception e) {

            log.error(
                    "GET /api/addresses - Failed to fetch addresses for userId: {}",
                    userId,
                    e
            );

            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "GET /api/addresses/{} - Fetching address for userId: {}",
                id,
                userId
        );

        try {
            AddressResponse address =
                    addressService.getById(id, userId);

            log.debug(
                    "GET /api/addresses/{} - Found address for userId: {}",
                    id,
                    userId
            );

            return ResponseEntity.ok(address);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "GET /api/addresses/{} - Address not found for userId: {}",
                    id,
                    userId
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "GET /api/addresses/{} - Unexpected error for userId: {}",
                    id,
                    userId,
                    e
            );

            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> update(
            @PathVariable String id,
            @RequestBody java.util.Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "PUT /api/addresses/{} - Updating address for userId: {}",
                id,
                userId
        );

        try {
            AddressResponse updated =
                    addressService.updatePartial(
                            id,
                            request,
                            userId
                    );

            log.info(
                    "PUT /api/addresses/{} - Successfully updated for userId: {}",
                    id,
                    userId
            );

            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "PUT /api/addresses/{} - Update failed - {}",
                    id,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "PUT /api/addresses/{} - Unexpected error for userId: {}",
                    id,
                    userId,
                    e
            );

            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "DELETE /api/addresses/{} - Deleting address for userId: {}",
                id,
                userId
        );

        try {
            addressService.delete(id, userId);

            log.info(
                    "DELETE /api/addresses/{} - Successfully deleted for userId: {}",
                    id,
                    userId
            );

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {

            log.warn(
                    "DELETE /api/addresses/{} - Delete failed - {}",
                    id,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "DELETE /api/addresses/{} - Unexpected error for userId: {}",
                    id,
                    userId,
                    e
            );

            throw e;
        }
    }
}