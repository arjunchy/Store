package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.WishlistRequest;
import com.ecommerce.backend.dto.response.WishlistResponse;
import com.ecommerce.backend.service.wishlist.WishlistService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@Slf4j
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @PostMapping
    public ResponseEntity<WishlistResponse> addToWishlist(
            @Valid @RequestBody WishlistRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "POST /api/wishlist - Adding product {} for userId: {}",
                request.productId(),
                userId
        );

        try {
            WishlistResponse response =
                    wishlistService.addToWishlist(userId, request);

            log.info(
                    "POST /api/wishlist - Successfully added wishlist item with id: {} for userId: {}",
                    response.id(),
                    userId
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException e) {

            log.warn(
                    "POST /api/wishlist - Failed for userId: {} - {}",
                    userId,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "POST /api/wishlist - Unexpected error for userId: {}",
                    userId,
                    e
            );

            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "GET /api/wishlist - Fetching wishlist for userId: {}",
                userId
        );

        try {
            List<WishlistResponse> wishlist =
                    wishlistService.getWishlist(userId);

            log.debug(
                    "GET /api/wishlist - Retrieved {} items for userId: {}",
                    wishlist.size(),
                    userId
            );

            return ResponseEntity.ok(wishlist);

        } catch (Exception e) {

            log.error(
                    "GET /api/wishlist - Failed for userId: {}",
                    userId,
                    e
            );

            throw e;
        }
    }

    @DeleteMapping("/{wishlistId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable String wishlistId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "DELETE /api/wishlist/{} - Removing for userId: {}",
                wishlistId,
                userId
        );

        try {
            wishlistService.removeFromWishlist(userId, wishlistId);

            log.info(
                    "DELETE /api/wishlist/{} - Successfully removed for userId: {}",
                    wishlistId,
                    userId
            );

            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {

            log.warn(
                    "DELETE /api/wishlist/{} - Failed for userId: {} - {}",
                    wishlistId,
                    userId,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "DELETE /api/wishlist/{} - Unexpected error for userId: {}",
                    wishlistId,
                    userId,
                    e
            );

            throw e;
        }
    }
}